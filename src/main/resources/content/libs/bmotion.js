function loadScript(url, callback){
    var script = document.createElement("script")
    script.type = "text/javascript";
    if (script.readyState){  //IE
        script.onreadystatechange = function(){
            if (script.readyState == "loaded" ||
                    script.readyState == "complete"){
                script.onreadystatechange = null;
                callback();
            }
        };
    } else {  //Others
        script.onload = function(){
            callback();
        };
    }
    script.src = url;
    document.getElementsByTagName("head")[0].appendChild(script);
}

var script = document.currentScript.getAttribute("data-script")
loadScript("/bms/libs/requirejs/require.js", function() {
    require.config({
        shim : {
            "socketio": {
                exports: 'io'
            },
            "bootstrap" : {
                "deps" :['jquery']
            },
            "jquery-ui" : {
                exports: "$",
                "deps" :['jquery']
            }
        },
        paths: {
            "jquery" : "/bms/libs/jquery/jquery-1.11.0.min",
            "jquery-ui" : "/bms/libs/jquery-ui/jquery-ui.min",
            "bootstrap" :  "/bms/libs/bootstrap/js/bootstrap.min",
            "socketio" : "/bms/libs/socket.io/socket.io",
            "bmotion" : "/bms/libs/bmotion/bmotion"
        },
        callback: function() {
            require(['bmotion'], function(bmotion) {
                require([script]);
            });
        }
    });
})
loadCss("/bms/libs/jquery-ui/jquery-ui.min.css")
loadCss("/bms/libs/jquery-ui/jquery-ui.theme.min.css")
loadCss("/bms/libs/bootstrap/css/bootstrap.min.css")
loadCss("/bms/libs/bmotion/bmotion.css")
