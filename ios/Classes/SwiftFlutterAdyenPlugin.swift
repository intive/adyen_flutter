import Flutter
import UIKit
import Adyen
import Adyen3DS2
import Foundation
import AdyenNetworking
import PassKit

struct PaymentError: Error {
    var error: String
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
    var topController: UIViewController?
    var mResult: FlutterResult?
    
    // Header
    var baseURL: String = ""
    var accessToken: String = ""
    var publicKey: String = ""
    // Config drop-in
    var clientKey: String = ""
    var environment: String = ""
    var appleMerchantID: String = ""
    // Payment params
    var locale: String = ""
    var shopperReference: String?
    var returnUrl: String?
    var amount: String = ""
    var lineItems: [LineItem]?
    var currency: String = ""
    var merchantAccount: String = ""
    var reference: String?
    var threeDS2RequestData: ThreeDS2RequestData?
    var additionalData: AdditionalData?
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard call.method.elementsEqual("openDropIn") else { return }

        let arguments = call.arguments as? [String: Any]
        mResult = result
        baseURL = arguments?["baseUrl"] as? String ?? ""
        publicKey = arguments?["publicKey"] as? String ?? ""
        accessToken = arguments?["accessToken"] as? String ?? ""
         
        clientKey = arguments?["clientKey"] as? String ?? ""
        environment = arguments?["environment"] as? String ?? ""
        appleMerchantID = arguments?["appleMerchantID"] as? String ?? ""
        
        locale = arguments?["locale"] as? String ?? "HK"
        shopperReference = arguments?["shopperReference"] as? String
        returnUrl = arguments?["returnUrl"] as? String
        amount = arguments?["amount"] as? String ?? "0"
        currency = arguments?["currency"] as? String ?? "HKD"
        merchantAccount = arguments?["merchantAccount"] as? String ?? ""
        reference = arguments?["reference"] as? String
        
        let paymentMethods = arguments?["paymentMethods"] as? String
        guard let paymentData = paymentMethods?.data(using: .utf8),
              let paymentMethods = try? JSONDecoder().decode(PaymentMethods.self, from: paymentData) else {
            return
        }
        
        let _lineItems = arguments?["lineItem"] as? [[String:String]]
        if let data = try? JSONEncoder().encode(_lineItems) {
           lineItems = try? JSONDecoder().decode([LineItem].self, from: data)
        }
        
        let _threeDS2RequestData = arguments?["threeDS2RequestData"] as? [String: String]
        if let data = try? JSONEncoder().encode(_threeDS2RequestData){
            threeDS2RequestData = try? JSONDecoder().decode(ThreeDS2RequestData.self, from: data)
        }
        
        let _additionalData = arguments?["additionalData"] as? [String: String]
        if let data = try? JSONEncoder().encode(_additionalData){
            additionalData = try? JSONDecoder().decode(AdditionalData.self, from: data)
        }

        var ctx = Environment.test
        if(environment == "LIVE_US") {
            ctx = Environment.liveUnitedStates
        } else if (environment == "LIVE_AUSTRALIA"){
            ctx = Environment.liveAustralia
        } else if (environment == "LIVE_EUROPE"){
            ctx = Environment.liveEurope
        }

        let dropInComponentStyle = DropInComponent.Style()

        do {
            let apiContext = try APIContext(environment: ctx, clientKey: clientKey)
            
            let ammount = Amount(value: Decimal(string: amount) ?? 0, currencyCode: currency)
            let payment = Payment(amount: ammount, countryCode: locale)
            let adyenContext = AdyenContext(apiContext: apiContext, payment: payment)
            
            let configuration = DropInComponent.Configuration(style: dropInComponentStyle)
            configuration.card.showsHolderNameField = true
            configuration.card.showsStorePaymentMethodField = false
            // Apple pay
            do {
                let label = (lineItems != nil && lineItems!.count == 1) ? lineItems!.first!.description : "Total"
                let total = PKPaymentSummaryItem(label: label, amount: NSDecimalNumber(string: amount), type: .final)
                let paymentSummaryItems = [total]
                
                let payment = try ApplePayPayment(
                    countryCode: locale,
                    currencyCode: currency,
                    summaryItems: paymentSummaryItems
                )
                let merchantIdentifier = appleMerchantID
                let applePayConfiguration = ApplePayComponent.Configuration(payment: payment, merchantIdentifier: merchantIdentifier)
                configuration.applePay = applePayConfiguration
            } catch {
                print("Fail to config apple pay")
            }
            
            dropInComponent = DropInComponent(paymentMethods: paymentMethods, context: adyenContext, configuration: configuration)
            dropInComponent?.delegate = self

            if var topController = UIApplication.shared.keyWindow?.rootViewController, let dropIn = dropInComponent {
                self.topController = topController
                while let presentedViewController = topController.presentedViewController{
                    topController = presentedViewController
                }
                topController.present(dropIn.viewController, animated: true)
            }
        } catch {
            print(error.localizedDescription)
        }
    }
}

extension SwiftFlutterAdyenPlugin: DropInComponentDelegate {
    
    public func didSubmit(_ data: Adyen.PaymentComponentData, from component: Adyen.PaymentComponent, in dropInComponent: Adyen.AnyDropInComponent) {
        
        NSLog("I'm here")
        guard let url = URL(string: baseURL + "payments") else { return }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue(publicKey, forHTTPHeaderField: "x-API-key")
        request.addValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")

        let amountAsInt = Decimal(string: amount) ?? 0
        let paymentMethod = data.paymentMethod.encodable
        let paymentRequest = PaymentRequestV69(
            paymentMethod: paymentMethod,
            amount: amountAsInt,
            reference: reference ?? UUID().uuidString,
            returnUrl: returnUrl ?? "",
            merchantAccount: merchantAccount,
            currency: currency,
            shopperReference: shopperReference ?? "",
            countryCode: locale,
            additionalData: additionalData,
            lineItems: lineItems,
            threeDS2RequestData: threeDS2RequestData)
        do {
            let jsonData = try JSONEncoder().encode(paymentRequest)
            print(String(data: jsonData, encoding: .utf8) ?? "")
            
            request.httpBody = jsonData
            URLSession.shared.dataTask(with: request) { data, response, error in
                if let data = data {
                    self.finish(data: data, component: dropInComponent)
                }
                if error != nil {
                    self.didFail(with: PaymentError(error: error?.localizedDescription ?? "didSubmit"), from: component, in: dropInComponent)
                }
            }.resume()
        } catch {
            didFail(with: PaymentError(error: "didSubmit - parse json"), from: component, in: dropInComponent)
        }
    }
    
    public func didProvide(_ data: Adyen.ActionComponentData, from component: Adyen.ActionComponent, in dropInComponent: Adyen.AnyDropInComponent) {
        
        guard let url = URL(string: baseURL + "payments/details") else { return }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue(publicKey, forHTTPHeaderField: "x-API-key")
        request.addValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        do {
            let detailsRequestData = try JSONEncoder().encode(data.details.encodable)
            let detailsRequestString = String(data: detailsRequestData, encoding: .utf8) ?? ""
            let lineItemRequestData = try JSONEncoder().encode(lineItems)
            let lineItemRequestString = String(data: lineItemRequestData, encoding: .utf8) ?? ""
            let body  = """
            {
                "details": \(detailsRequestString),
                "lineItems": \(lineItemRequestString)
            }
            """
            request.httpBody = body.data(using: .utf8)
            
            URLSession.shared.dataTask(with: request) { data, response, error in
                if let response = response as? HTTPURLResponse {
                    if (response.statusCode != 200) {
                        self.didFail(with: PaymentError(error: "didProvide"), from: component, in:  dropInComponent)
                    }
                }
                if let data = data {
                    self.finish(data: data, component: dropInComponent)
                }

            }.resume()
        } catch {
            self.didFail(with: PaymentError(error: "didProvide - parse json"), from: component, in: dropInComponent)
        }
    }
    
    public func didComplete(from component: Adyen.ActionComponent, in dropInComponent: Adyen.AnyDropInComponent) {
        component.stopLoadingIfNeeded()
    }
    
    public func didCancel(component: PaymentComponent, from dropInComponent: AnyDropInComponent) {
        didFail(with: PaymentCancelled(), from: dropInComponent)
    }
    
    public func didFail(with error: Error, from component: Adyen.PaymentComponent, in dropInComponent: Adyen.AnyDropInComponent) {
        print("[didSubmit] \(error.localizedDescription)")
        didFail(with: error, from: dropInComponent)
    }
    
    public func didFail(with error: Error, from component: Adyen.ActionComponent, in dropInComponent: Adyen.AnyDropInComponent) {
        print("[didProvide] \(error.localizedDescription)")
        didFail(with: error, from: dropInComponent)
    }
    
    public func didFail(with error: Error, from dropInComponent: Adyen.AnyDropInComponent) {
        DispatchQueue.main.async {
            if (error is PaymentCancelled) {
                self.mResult?("PAYMENT_CANCELLED")
            } else if let componentError = error as? ComponentError, componentError == ComponentError.cancelled {
                self.mResult?("PAYMENT_CANCELLED")
            }else {
                if let e = error as? PaymentError {
                    self.mResult?("PAYMENT_ERROR_\(e.error)")
                } else {
                    self.mResult?("PAYMENT_ERROR_\(error.localizedDescription)")
                }
            }
            self.topController?.dismiss(animated: true, completion: nil)
        }
    }
    
    func finish(data: Data, component: Adyen.AnyDropInComponent) {
        DispatchQueue.main.async {
            print(String(data: data, encoding: .utf8) ?? "")
            guard let response = try? JSONDecoder().decode(PaymentsResponse.self, from: data) else {
                self.didFail(with: PaymentError(error: "finish - parse json"), from: component)
                return
            }
            if let action = response.action {
                component.stopLoadingIfNeeded()
                if let component = component as? DropInComponent {
                    component.handle(action)
                }
                
            } else {
                component.stopLoadingIfNeeded()
                if response.resultCode == .authorised || response.resultCode == .received || response.resultCode == .pending, let result = self.mResult {
                    result(response.resultCode.rawValue)
                    self.topController?.dismiss(animated: true, completion: nil)

                } else if (response.resultCode == .error || response.resultCode == .refused) {
                    self.didFail(with: PaymentError(error: "finish - fail"), from: component)
                }
                else {
                    self.didFail(with: PaymentCancelled(), from: component)
                }
            }
        }
    }
}

struct DetailsRequest: Encodable {
    let lineItems: [LineItem]
    let details: AnyEncodable
}

struct PaymentRequestV69: Encodable {
    let amount: Amount
    let reference: String
    let paymentMethod: AnyEncodable
    let returnUrl: String
    let merchantAccount: String
    let shopperReference: String
    let countryCode: String
    let channel: String
    let additionalData: AdditionalData?
    let lineItems: [LineItem]
    let threeDS2RequestData: ThreeDS2RequestData?

    init(paymentMethod: AnyEncodable, amount: Decimal, reference: String, returnUrl: String, merchantAccount: String, currency: String, shopperReference: String, countryCode: String , additionalData: AdditionalData?, lineItems: [LineItem]?, threeDS2RequestData: ThreeDS2RequestData?) {
        self.paymentMethod = paymentMethod
        self.amount = Amount(value: amount, currencyCode: currency)
        self.returnUrl = returnUrl
        self.merchantAccount = merchantAccount
        self.reference = reference
        self.shopperReference = shopperReference
        self.countryCode = countryCode
        self.channel = "ios"
        self.additionalData = additionalData
        self.lineItems = lineItems ?? []
        self.threeDS2RequestData = threeDS2RequestData
    }
}

struct LineItem: Codable {
    let id: String
    let quantity: String
    let description: String
}

struct ThreeDS2RequestData: Codable {
    let deviceChannel: String
    let challengeIndicator: String
}

struct AdditionalData: Codable {
    let allow3DS2: String
    let executeThreeD: String
}

internal struct PaymentsResponse: Response {

    internal let resultCode: ResultCode

    internal let action: Action?

    internal init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.resultCode = try container.decode(ResultCode.self, forKey: .resultCode)
        self.action = try container.decodeIfPresent(Action.self, forKey: .action)
    }

    private enum CodingKeys: String, CodingKey {
        case resultCode
        case action
    }

}

internal extension PaymentsResponse {

    // swiftlint:disable:next explicit_acl
    enum ResultCode: String, Decodable {
        case authorised = "Authorised"
        case refused = "Refused"
        case pending = "Pending"
        case cancelled = "Cancelled"
        case error = "Error"
        case received = "Received"
        case redirectShopper = "RedirectShopper"
        case identifyShopper = "IdentifyShopper"
        case challengeShopper = "ChallengeShopper"
        case presentToShopper = "PresentToShopper"
    }

}
