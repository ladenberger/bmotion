require.config({
    map: {
      '*': {
        'css': '/bms/libs/require-css/css.min.js'
      }
    },
    shim : {
        "socketio": {
            exports: 'io'
        },
        "bootstrap" : {
            "deps": ['jquery']
        },
        "jquery-ui" : {
            exports: "$",
            "deps": ['jquery']
        },
        "jquery-cookie" : {
            exports: "$",
            "deps": ['jquery']
        }
    },
    paths: {
        "jquery" : "/bms/libs/jquery/jquery-1.11.0.min",
        "jquery-ui" : "/bms/libs/jquery-ui/jquery-ui.min",
        "jquery-cookie" : "/bms/libs/jquery-cookie/jquery.cookie",
        "bootstrap" :  "/bms/libs/bootstrap/js/bootstrap.min",
        "socketio" : "/bms/libs/socket.io/socket.io",
        "common" : "/bms/libs/common/common",
        "bmotion" : "/bms/libs/bmotion/bmotion",
        "bootstrap-css" : "/bms/libs/bootstrap/css/bootstrap.min",
        "jquery-ui-css" : "/bms/libs/jquery-ui/jquery-ui.min",
        "jquery-ui-theme-css" : "/bms/libs/jquery-ui/jquery-ui.theme.min",
        "bmotion-css" : "/bms/libs/bmotion/bmotion"
    }
});
define(["css!jquery-ui-css","css!jquery-ui-theme-css","css!bootstrap-css","css!bmotion-css","bootstrap","jquery-ui","jquery-cookie","socketio"], function() {

    $("body").append('<div class="modal" id="loadingModal" tabindex="-1" role="dialog" aria-labelledby="loadingModalLabel" aria-hidden="true">'+
        '<div class="modal-dialog modal-vertical-centered">'+
        '    <div class="modal-content">'+
        '        <div class="modal-header">'+
        '            <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>'+
        '            <h4 class="modal-title" id="myModalLabel">Loading visualisation ...</h4>'+
        '        </div>'+
        '        <div class="modal-body" style="text-align:center">'+
        '            <p><img src="/bms/libs/bmotion/bmotion.png" /></p>'+
        '            <p><img src="/bms/libs/bmotion/spinner3-bluey.gif" /></p>'+
        '        </div>'+
        '    </div>'+
        '</div>'+
        '</div>')

    $("body").append('<nav class="navbar navbar-default navbar-fixed-bottom" role="navigation">'+
    '        <div class="container-fluid">'+
    '            <div class="navbar-header">'+
    '                <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">'+
    '                    <span class="sr-only">Toggle navigation</span>'+
    '                    <span class="icon-bar"></span>'+
    '                    <span class="icon-bar"></span>'+
    '                    <span class="icon-bar"></span>'+
    '                </button>'+
    '                <a class="navbar-brand" href="#" id="bmotion-label">BMotion Studio</a>'+
    '            </div>'+
    '            <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">'+
    '                <ul class="nav navbar-nav navbar-right" id="bmotion-navigation">'+
    '                    <li class="dropdown">'+
    '                        <a href="#" class="dropdown-toggle" data-toggle="dropdown">Model <span class="caret"></span></a>'+
    '                        <ul class="dropdown-menu" role="menu">'+
    '                            <li><a id="bt_reloadModel" href="#">Reload</a></li>'+
    '                        </ul>'+
    '                    </li>'+
    '                </ul>'+
    '            </div>'+
    '        </div>'+
    '    </nav>')

    $("#bt_reloadModel").click(function() {
        socket.emit('reloadModel', function() {
            console.log("reload call back")
        });
    });

    $('#loadingModal').modal('show')

    // ---------------------
    // Establish client socket
    // ---------------------
    var socket = io.connect('http://localhost:9090');
    socket.on('connect', function() {
        var event = {
            templateUrl: document.URL,
            scriptPath: $("meta[name='bms.script']").attr("content"),
            modelPath: $("meta[name='bms.model']").attr("content"),
            tool: $("meta[name='bms.tool']").attr("content")
        };
        socket.emit('initSession', event, function() {

            // Callback after initialising BMotion session
            // Loading visualisation finished
            $('#loadingModal').modal('hide')

        });
    });

    socket.on('applyTransformers', function(data) {
      var d1 = JSON.parse(data)
      var i1 = 0
      for (; i1 < d1.length; i1++) {
          var t = d1[i1]
          if(t.selector) {
              var selector = $(t.selector)
              var content = t.content
              if(content != undefined) selector.html(content)
              selector.attr(t.attributes)
              selector.css(t.styles)
          }
      }
    });
    // ---------------------

    // ---------------------
    // Return BMotion API functions
    // ---------------------
    return {
        socket: socket,
        callMethod: function(n,d) {
            var fSuccessFn = function(result) {}
            if(d.success !== "undefined") {
                fSuccessFn = d.success
            }
            var df = {
                name: n,
                data: d.data
            };
            socket.emit('callGroovyMethod', df, fSuccessFn);
        }
    }

});