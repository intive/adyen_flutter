import Flutter
import UIKit
import Adyen
import Adyen3DS2
import Foundation

struct PaymentError: Error {
    
}
struct PaymentCancelled: Error {
    
}
public class SwiftFlutterAdyenPlugin: NSObject, FlutterPlugin {
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "flutter_adyen", binaryMessenger: registrar.messenger())
        let instance = SwiftFlutterAdyenPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    var dropInComponent: DropInComponent?
    var baseURL: String?
    var authToken: String?
    var merchantAccount: String?
    var clientKey: String?
    var currency: String?
    var amount: String?
    var returnUrl: String?
    var reference: String?
    var mResult: FlutterResult?
    var topController: UIViewController?
    var environment: String?
    var shopperReference: String?
    var lineItemJson: [String: String]?
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard call.method.elementsEqual("openDropIn") else { return }
        
        let arguments = call.arguments as? [String: Any]
        let paymentMethodsResponse = arguments?["paymentMethods"] as? String
        baseURL = arguments?["baseUrl"] as? String
        
        clientKey = arguments?["clientKey"] as? String
        currency = arguments?["currency"] as? String
        amount = arguments?["amount"] as? String
        lineItemJson = arguments?["lineItem"] as? [String: String]
        environment = arguments?["environment"] as? String
        reference = arguments?["reference"] as? String
        returnUrl = arguments?["returnUrl"] as? String
        shopperReference = arguments?["shopperReference"] as? String
        
        mResult = result
        
        guard let paymentData = paymentMethodsResponse?.data(using: .utf8),
              let paymentMethods = try? JSONDecoder().decode(PaymentMethods.self, from: paymentData) else {
            return
        }
        
        let configuration = DropInComponent.PaymentMethodsConfiguration()
        configuration.clientKey = clientKey
        dropInComponent = DropInComponent(paymentMethods: paymentMethods, paymentMethodsConfiguration: configuration)
        dropInComponent?.delegate = self
        dropInComponent?.environment = .test
        
        if(environment == "LIVE_US") {
            dropInComponent?.environment = .liveUnitedStates
        } else if (environment == "LIVE_AUSTRALIA"){
            dropInComponent?.environment = .liveAustralia
        } else if (environment == "LIVE_EUROPE"){
            dropInComponent?.environment = .liveEurope
        }
        
        
        if var topController = UIApplication.shared.keyWindow?.rootViewController, let dropIn = dropInComponent {
            self.topController = topController
            while let presentedViewController = topController.presentedViewController{
                topController = presentedViewController
            }
            topController.present(dropIn.viewController, animated: true)
        }
    }
}

extension SwiftFlutterAdyenPlugin: DropInComponentDelegate {
    
    public func didCancel(component: PresentableComponent, from dropInComponent: DropInComponent) {
        self.didFail(with: PaymentCancelled(), from: dropInComponent)
    }
    
    public func didSubmit(_ data: PaymentComponentData, from component: DropInComponent) {
        guard let baseURL = baseURL, let url = URL(string: baseURL + "payments") else { return }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let amountAsInt = Int(amount ?? "0")
        // prepare json data
        let paymentMethod = data.paymentMethod.encodable
        let lineItem = try? JSONDecoder().decode(LineItem.self, from: JSONSerialization.data(withJSONObject: lineItemJson ?? ["":""]) )
        if lineItem == nil {
            self.didFail(with: PaymentError(), from: component)
            return
        }
        let paymentRequest = PaymentRequest( paymentMethod: paymentMethod, lineItem: lineItem ?? LineItem(id: "", description: ""), currency: currency ?? "", amount: amountAsInt ?? 0,reference: reference ?? "", returnUrl: returnUrl ?? "", storePayment: data.storePaymentMethod, shopperReference: shopperReference)

        do {
            let jsonData = try JSONEncoder().encode(paymentRequest)
            
            request.httpBody = jsonData
            URLSession.shared.dataTask(with: request) { data, response, error in
                if let data = data {
                    self.finish(data: data, component: component)
                }
                if let error = error {
                    self.didFail(with: PaymentError(), from: component)
                }
            }.resume()
            
            
        } catch {
            didFail(with: PaymentError(), from: component)
        }
        
    }
    
    func finish(data: Data, component: DropInComponent) {
        let paymentResponseJson = ((try? JSONSerialization.jsonObject(with: data, options: .allowFragments) as? Dictionary<String,Any>) as Dictionary<String, Any>??)
        DispatchQueue.main.async {
            if let dict = paymentResponseJson, let action = dict?["action"] {
                let act = try? JSONDecoder().decode(Action.self, from: JSONSerialization.data(withJSONObject: action, options: .sortedKeys))
                if let act = act {
                    component.handle(act)
                }
            } else if let dict = paymentResponseJson, let resultCode = dict?["resultCode"] as? String {
                let success = resultCode == "Authorised" || resultCode == "Received" || resultCode == "Pending"
                component.stopLoading()
                if success, let result = self.mResult {
                    
                    result(resultCode)
                    self.topController?.dismiss(animated: false, completion: nil)
                    
                } else {
                    DispatchQueue.main.async {
                        self.mResult?(resultCode)
                        self.topController?.dismiss(animated: false, completion: nil)
                    }
                }
            } else {
                self.didFail(with: PaymentError(), from: component)
            }
        }
    }
    
    public func didProvide(_ data: ActionComponentData, from component: DropInComponent) {
        guard let baseURL = baseURL, let url = URL(string: baseURL + "payments/details/") else { return }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let json: [String: Any] = ["details": data.details.encodable,"paymentData": data.paymentData]
        let jsonData = try? JSONSerialization.data(withJSONObject: json)
        request.httpBody = jsonData
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let data = data {
                self.finish(data: data, component: component)
            }
        }.resume()
    }
    
    public func didFail(with error: Error, from component: DropInComponent) {
        
        DispatchQueue.main.async {
            if let error = error as? ComponentError, error == ComponentError.cancelled {
                self.mResult?("PAYMENT_CANCELLED")
            }else {
                self.mResult?("PAYMENT_ERROR")
            }
            self.topController?.dismiss(animated: false, completion: nil)
        }
    }
}

struct PaymentRequest : Encodable {
    let paymentMethod: AnyEncodable
    let lineItems: [LineItem]
    let channel: String = "iOS"
    let additionalData = ["allow3DS2":"false"]
    let amount: Amount
    let reference: String
    let returnUrl: String
    let shopperReference: String?
    
    init(paymentMethod: AnyEncodable, lineItem: LineItem, currency: String, amount: Int, reference: String, returnUrl: String, storePayment: Bool, shopperReference: String?) {
        self.paymentMethod = paymentMethod
        self.lineItems = [lineItem]
        self.amount = Amount(currency: currency, value: amount)
        self.reference = reference
        self.returnUrl = returnUrl
        if(storePayment) {self.shopperReference = shopperReference} else {self.shopperReference = nil}
    }
    
}

struct LineItem: Codable {
    let id: String
    let description: String
}

struct Amount: Codable {
    let currency: String
    let value: Int
}

