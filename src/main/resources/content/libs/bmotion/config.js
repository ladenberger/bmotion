require.config({
    map: {
        '*': {
            'css': '/bms/libs/require-css/css.min.js'
        }
    },
    paths: {
        "angular": "/bms/libs/angular/angular.min",
        "angular-route": "/bms/libs/angular/angular-route.min",
        "angularAMD": "/bms/libs/angular/angularAMD.min",
        "bms": "/bms/libs/bmotion/bmotion",
        "ngBMotion": "/bms/libs/bmotion/ngBMotion",
        "jquery": "/bms/libs/jquery/jquery-1.11.0.min",
        "socketio": "/bms/libs/socket.io/socket.io",
        "bootstrap": "/bms/libs/bootstrap/js/bootstrap.min",
        "bootstrap-css": "/bms/libs/bootstrap/css/bootstrap.min",
        "bmotion-css": "/bms/libs/bmotion/bmotion"
    },
    shim: {
        'jquery': {'exports': '$'},
        'angular': {'exports': 'angular'},
        'angularAMD': ["angular"],
        'angular-route': ["angular"],
        'socketio': {'exports': 'io'},
        'ngBMotion': ["bms", "socketio", "bootstrap"],
        'bootstrap': ["jquery"],
        'bms': {'exports': 'bms'}
    }
});
define(function () {

    return {
        socket: {
            protocol: document.location.protocol,
            host: document.location.hostname,
            port: 9090
        },
        model: "",
        script: "",
        tool: "BAnimation"
    }

});