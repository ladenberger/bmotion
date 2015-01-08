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
        "jquery-ui": "/bms/libs/jquery-ui/jquery-ui.min",
        "jquery-cookie": "/bms/libs/jquery-cookie/jquery.cookie",
        "bootstrap": "/bms/libs/bootstrap/js/bootstrap.min",
        "bootstrap-css": "/bms/libs/bootstrap/css/bootstrap.min",
        "jquery-ui-css": "/bms/libs/jquery-ui/jquery-ui.min",
        "jquery-ui-theme-css": "/bms/libs/jquery-ui/jquery-ui.theme.min",
        "tooltipster": "/bms/libs/tooltipster/jquery.tooltipster.min",
        "tooltipster-css": "/bms/libs/tooltipster/tooltipster",
        "tooltipster-shadow-css": "/bms/libs/tooltipster/themes/tooltipster-shadow",
        "bmotion-css": "/bms/libs/bmotion/bmotion"
    },
    shim: {
        'jquery': {'exports': '$'},
        'jquery-ui': ['jquery'],
        'angular': {'exports': 'angular'},
        'angularAMD': ["angular"],
        'angular-route': ["angular"],
        'socketio': {'exports': 'io'},
        'ngBMotion': ["bms", "socketio", "bootstrap", "jquery-cookie"],
        'bootstrap': ["jquery"],
        'tooltipster': ["jquery"],
        'bms': {'exports': 'bms'}
    }
});
