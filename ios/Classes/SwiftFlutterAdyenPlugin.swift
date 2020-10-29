import Flutter
import UIKit
import Adyen
import Adyen3DS2
import Foundation

struct PaymentError: Error {
    
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
    var shopperReference: String?
    var reference: String?
    var mResult: FlutterResult?
    var topController: UIViewController?
    var environment: String?
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard call.method.elementsEqual("openDropIn") else { return }
        
        let arguments = call.arguments as? [String: Any]
        let paymentMethodsResponse = arguments?["paymentMethods"] as? String
        baseURL = arguments?["baseUrl"] as? String
        authToken = arguments?["authToken"] as? String
        merchantAccount = arguments?["merchantAccount"] as? String
        clientKey = arguments?["clientKey"] as? String
        currency = arguments?["currency"] as? String
        amount = arguments?["amount"] as? String
        returnUrl = arguments?["iosReturnUrl"] as? String
        shopperReference = arguments?["shopperReference"] as? String
        reference = arguments?["reference"] as? String
        environment = arguments?["environment"] as? String
        mResult = result
        
        guard let paymentData = paymentMethodsResponse?.data(using: .utf8),
            let paymentMethods = try? JSONDecoder().decode(PaymentMethods.self, from: paymentData) else {
                return
        }
        
        let configuration = DropInComponent.PaymentMethodsConfiguration()
        configuration.clientKey = clientKey
        dropInComponent = DropInComponent(paymentMethods: paymentMethods, paymentMethodsConfiguration: configuration)
        dropInComponent?.delegate = self
        if(environment == "PROD") {
            dropInComponent?.environment = .live
        } else {
            dropInComponent?.environment = .test
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
    
    public func didSubmit(_ data: PaymentComponentData, from component: DropInComponent) {
        guard let baseURL = baseURL, let url = URL(string: baseURL + "payments/") else { return }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("\(authToken ?? "")", forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let amountAsDouble = Double(amount ?? "0.0")
        // prepare json data
        let paymentMethod = data.paymentMethod.encodable
        let json: [String: Any] = ["paymentMethod": paymentMethod,
                                   "amount": ["currency":currency ?? "", "value":amountAsDouble ?? 0.0],
                                   "channel": "iOS",
                                   "merchantAccount": merchantAccount ?? "",
                                   "reference": reference ?? "",
                                   "returnUrl": returnUrl ?? "" + "://",
                                   "storePaymentMethod": false,
                                   "additionalData": ["allow3DS2":"false"]]
        
        do {
            if JSONSerialization.isValidJSONObject(json) {
                let jsonData = try JSONSerialization.data(withJSONObject: json)
                request.httpBody = jsonData
                URLSession.shared.dataTask(with: request) { data, response, error in
                    if let data = data {
                        self.finish(data: data, component: component)
                    }
                    }.resume()
            } else {
                didFail(with: PaymentError(), from: component)
            }
            
        } catch {
            didFail(with: PaymentError(), from: component)
        }

    }
    
    func finish(data: Data, component: DropInComponent) {
        let paymentResponseJson = ((try? JSONSerialization.jsonObject(with: data, options: .allowFragments) as? Dictionary<String,Any>) as Dictionary<String, Any>??)
       
        if let dict = paymentResponseJson, let action = dict?["action"] {
                let act = try? JSONDecoder().decode(Action.self, from: JSONSerialization.data(withJSONObject: action, options: .sortedKeys))
                if let act = act {
                    component.handle(act)
                }
        } else if let dict = paymentResponseJson, let resultCode = dict?["resultCode"] as? String {
                let success = resultCode == "Authorised" || resultCode == "Received" || resultCode == "Pending"
                component.stopLoading()
                if success, let result = self.mResult {
                    result("SUCCESS")
                    DispatchQueue.global(qos: .background).async {
                        
                        // Background Thread
                        DispatchQueue.main.async {
                            self.topController?.dismiss(animated: false, completion: nil)
                        }
                    }
                } else {
                    self.mResult?("Failed with result code \(String(describing: resultCode))")
                    DispatchQueue.global(qos: .background).async {
                        
                        // Background Thread
                        
                        DispatchQueue.main.async {
                            self.topController?.dismiss(animated: false, completion: nil)
                        }
                    }
                
            }
        } else {
            didFail(with: PaymentError(), from: component)
        }
    }
    
    public func didProvide(_ data: ActionComponentData, from component: DropInComponent) {
        guard let baseURL = baseURL, let url = URL(string: baseURL + "payments/details/") else { return }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("\(authToken ?? "")", forHTTPHeaderField: "Authorization")
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
       self.mResult?("CANCELLED")
       DispatchQueue.global(qos: .background).async {
            
            // Background Thread
            
            DispatchQueue.main.async {
                self.topController?.dismiss(animated: false, completion: nil)
            }
        }
    }
}

extension UIViewController: PaymentComponentDelegate {
    
    public func didSubmit(_ data: PaymentComponentData, from component: PaymentComponent) {
        //performPayment(with: public  }
    }
    
    public func didFail(with error: Error, from component: PaymentComponent) {
        //performPayment(with: public  }
    }
    
}

extension UIViewController: ActionComponentDelegate {
    
    public func didFail(with error: Error, from component: ActionComponent) {
        //performPayment(with: public  }
    }
    
    public func didProvide(_ data: ActionComponentData, from component: ActionComponent) {
        //performPayment(with: public  }
    }
    
}
