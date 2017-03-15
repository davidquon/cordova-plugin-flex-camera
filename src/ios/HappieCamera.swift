import Foundation
import UIKit

@objc(HappieCamera) class HappieCamera : CDVPlugin, cameraDelegate  {

    //let cameraRoll: HappieCameraRoll = HappieCameraRoll();
    //let cameraView = HappieCameraViewController(nibName:"HappieCameraView", bundle:nil);

    var callBackId: String = "";

    func openCamera(_ command: CDVInvokedUrlCommand) {
        //cameraRoll.delegate = self;
        let params: AnyObject = command.arguments[0] as AnyObject!
        let qual: Int = params["quality"] as! Int
        HappieCameraJSON.setQuality(newQual: qual);
        let cameraVC: HappieCameraViewController = HappieCameraViewController(nibName:"HappieCameraView", bundle:nil);
        cameraVC.delegate = self;
        cameraVC.modalTransitionStyle = UIModalTransitionStyle.coverVertical;
        cameraVC.modalPresentationStyle = UIModalPresentationStyle.fullScreen;
        callBackId = command.callbackId;
        self.viewController?.present(cameraVC, animated: true, completion:nil)
    }

    func cameraFinished(_ controller: HappieCameraViewController){
        controller.dismiss(animated: true, completion: nil);
        var pluginResult: CDVPluginResult;
        pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "true")
        commandDelegate!.send(pluginResult, callbackId:callBackId)
    }

    func generateThumbnail(_ command: CDVInvokedUrlCommand){
        let params: AnyObject = command.arguments[0] as AnyObject!
        let name: String = params["name"] as! String

        self.commandDelegate.run { 
            let thumbGen = HappieCameraThumb();
            let res = thumbGen.createThumbAtPathWithName(name: name);
            if(res){
                let pluginResult: CDVPluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "true")
                self.commandDelegate!.send(pluginResult, callbackId:self.callBackId)
            }
            else {
                let pluginResult: CDVPluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "false")
                self.commandDelegate!.send(pluginResult, callbackId:self.callBackId)
            }
        }
    }

}
