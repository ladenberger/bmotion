require.config({
    map: {
        '*': {
            'css': '/bms/libs/require-css/css.min.js'
        }
    },
    shim: {
        "socketio": {
            exports: 'io'
        },
        "bootstrap": {
            "deps": ['jquery']
        },
        "jquery-ui": {
            exports: "$",
            "deps": ['jquery']
        },
        "jquery-cookie": {
            exports: "$",
            "deps": ['jquery']
        },
        "tooltipster": {
            exports: "$",
            "deps": ['jquery']
        }
    },
    paths: {
        "jquery": "/bms/libs/jquery/jquery-1.11.0.min",
        "jquery-ui": "/bms/libs/jquery-ui/jquery-ui.min",
        "jquery-cookie": "/bms/libs/jquery-cookie/jquery.cookie",
        "bootstrap": "/bms/libs/bootstrap/js/bootstrap.min",
        "socketio": "/bms/libs/socket.io/socket.io",
        "common": "/bms/libs/common/common",
        "bmotion": "/bms/libs/bmotion/bmotion",
        "bootstrap-css": "/bms/libs/bootstrap/css/bootstrap.min",
        "jquery-ui-css": "/bms/libs/jquery-ui/jquery-ui.min",
        "jquery-ui-theme-css": "/bms/libs/jquery-ui/jquery-ui.theme.min",
        "tooltipster": "/bms/libs/tooltipster/jquery.tooltipster.min",
        "tooltipster-css": "/bms/libs/tooltipster/tooltipster",
        "tooltipster-shadow-css": "/bms/libs/tooltipster/themes/tooltipster-shadow",
        "bmotion-css": "/bms/libs/bmotion/bmotion"
    }
});
define(["css!jquery-ui-css", "css!jquery-ui-theme-css", "css!bootstrap-css", "css!tooltipster-css", "css!tooltipster-shadow-css", "css!bmotion-css", "bootstrap", "jquery-ui", "jquery-cookie", "tooltipster", "socketio"], function () {

        // ---------------------
        // Establish client socket
        // ---------------------
        var socket = io.connect('http://localhost:9090');
        socket.on('connect', function () {

            $("body").append('<div class="modal" id="loadingModal" tabindex="-1" role="dialog" aria-labelledby="loadingModalLabel" aria-hidden="true">' +
            '<div class="modal-dialog modal-vertical-centered">' +
            '    <div class="modal-content">' +
            '        <div class="modal-header">' +
            '            <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>' +
            '            <h4 class="modal-title" id="loadingModalText">Loading visualisation ...</h4>' +
            '        </div>' +
            '        <div class="modal-body" style="text-align:center">' +
            '            <p><img src="/bms/libs/bmotion/bmotion.png" /></p>' +
            '            <p><img src="/bms/libs/bmotion/spinner3-bluey.gif" /></p>' +
            '        </div>' +
            '    </div>' +
            '</div>' +
            '</div>')

            $('#loadingModal').modal('show')

            var bmsSvg = {};
            $("object[data-bms=svg]").map(function () {
                bmsSvg[$(this).attr('data')] = ""
            });

            var event = {
                templateUrl: document.URL,
                scriptPath: $("meta[name='bms.script']").attr("content"),
                modelPath: $("meta[name='bms.model']").attr("content"),
                tool: $("meta[name='bms.tool']").attr("content"),
                bmsSvg: bmsSvg
            };
            socket.emit('initSession', event, function (data) {

                var standalone = data.standalone

                // Callback after initialising BMotion session

                if (standalone) {
                    $("body").append('<div title="SVG Editor" id="dialog_svgEditor"><iframe src="/bms/libs/bmseditor/index.html" frameBorder="0" id="iframe_svgEditor"></iframe></div>')
                    // Register socket in SVG editor
                    $('iframe#iframe_svgEditor').load(function () {
                        document.getElementById('iframe_svgEditor').contentWindow.methodDraw.socket = socket
                    });
                    $("body").append('<nav class="navbar navbar-default navbar-fixed-bottom" role="navigation">' +
                    '        <div class="container-fluid">' +
                    '            <div class="navbar-header">' +
                    '                <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">' +
                    '                    <span class="sr-only">Toggle navigation</span>' +
                    '                    <span class="icon-bar"></span>' +
                    '                    <span class="icon-bar"></span>' +
                    '                    <span class="icon-bar"></span>' +
                    '                </button>' +
                    '                <a class="navbar-brand" href="#" id="bmotion-label">BMotion Studio</a>' +
                    '            </div>' +
                    '            <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">' +
                    '                <ul class="nav navbar-nav navbar-right" id="bmotion-navigation">' +
                    '                    <li class="dropdown">' +
                    '                        <a href="#" id="bt_open_SvgEditor" class="dropdown-toggle" data-toggle="dropdown"> Edit SVG <span class="caret"></a>' +
                    '                        <ul class="dropdown-menu" role="menu" id="bmotion-navigation-svg">' +
                    '                        </ul>' +
                    '                    </li>' +
                    '                    <li class="dropdown">' +
                    '                        <a href="#" class="dropdown-toggle" data-toggle="dropdown">Model <span class="caret"></span></a>' +
                    '                        <ul class="dropdown-menu" role="menu" id="bmotion-navigation-model">' +
                    '                            <li><a id="bt_reloadModel" href="#"><i class="glyphicon glyphicon-refresh"></i> Reload</a></li>' +
                    '                        </ul>' +
                    '                    </li>' +
                    '                </ul>' +
                    '            </div>' +
                    '        </div>' +
                    '    </nav>')

                    $("#dialog_svgEditor").dialog({
                        dragStart: function (event, ui) {
                            $("#iframe_svgEditor").hide();
                        },
                        dragStop: function (event, ui) {
                            $("#iframe_svgEditor").show();
                        },
                        resize: function () {
                            $("#iframe_svgEditor").hide();
                        },
                        resizeStart: function () {
                            $("#iframe_svgEditor").hide();
                        },
                        resizeStop: function (ev, ui) {
                            $("#iframe_svgEditor").show();
                            fixSizeDialog($("#dialog_svgEditor"), $("#iframe_svgEditor"), 0, 0);
                        },
                        open: function (ev, ui) {
                            fixSizeDialog($("#dialog_svgEditor"), $("#iframe_svgEditor"), 0, 0);
                            $("#dialog_svgEditor").css('overflow', 'hidden'); //this line does the actual hiding
                        },
                        close: function (ev, ui) {
                            /*var svgEditor = document.getElementById('iframe_svgEditor').contentWindow.methodDraw
                             var svgCanvas = document.getElementById('iframe_svgEditor').contentWindow.svgCanvas
                             var svg = {
                             name: svgEditor.workingSvgFile,
                             content: svgCanvas.getSvgString()
                             };
                             var replaceSvg = $(svg.content)
                             replaceSvg.attr("data-svg", svg.name)
                             $("svg[data-svg='" + svg.name + "']").replaceWith(replaceSvg)
                             socket.emit('saveSvg', svg, function () {
                             });*/
                        },
                        autoOpen: false,
                        width: 900,
                        height: 600
                    });

                    $("#bt_reloadModel").click(function () {
                        $('#loadingModalText').html("Reloading model ...")
                        $('#loadingModal').modal('show')
                        socket.emit('reloadModel', function () {
                            $('#loadingModal').modal('hide')
                        });
                    });
                }

                // Replace linked SVG files with content and add corresponding menu items
                $.each(data.bmsSvg, function (i, v) {
                    var orgSvg = $("object[data='" + i + "']")
                    var newSvg = $(v)
                    newSvg.attr("data-svg", i)
                    orgSvg.replaceWith(newSvg)
                    if (standalone)
                        $("#bmotion-navigation-svg").append('<li><a href="#" data-svg="' + i + '"><i class="glyphicon glyphicon-pencil"></i> ' + i + '</a></li>')
                });

                if (standalone) {
                    // Open SVG Editor
                    var svgAItems = $("#bmotion-navigation-svg").find("a")
                    svgAItems.click(function () {
                        var svgFile = $(this).attr("data-svg")
                        socket.emit('initSvgEditor', svgFile, function (svg) {
                            $("#dialog_svgEditor").dialog("open");
                            //$("#dialog_svgEditor").data("svgFile", svgFile)
                            var svgEditor = document.getElementById('iframe_svgEditor').contentWindow.methodDraw
                            svgEditor.workingSvgFile = svgFile
                            svgEditor.loadFromString(svg)
                        });
                    })
                }

                socket.emit('initialisation');

                $('#loadingModal').modal('hide')

            });
        });

        socket.on('applyTransformers', function (data) {
            var d1 = JSON.parse(data)
            var i1 = 0
            for (; i1 < d1.length; i1++) {
                var t = d1[i1]
                if (t.selector) {
                    var selector = $(t.selector)
                    var content = t.content
                    if (content != undefined) selector.html(content)
                    selector.attr(t.attributes)
                    selector.css(t.styles)
                }
            }
        });

        socket.on('checkObserver', function (trigger) {
            $(document).trigger({
                type: "checkObserver_" + trigger
            });
        });

        // ---------------------

        var executeEvent = function (options, origin) {
            var settings = $.extend({
                events: [],
                callback: function () {
                }
            }, options);
            socket.emit("executeEvent", {data: settings}, function (data) {
                origin !== undefined ? settings.callback.call(this, origin, data) : settings.callback.call(this, data)
            });
        }

        var callMethod = function (options, origin) {
            var settings = $.extend({
                name: "",
                callback: function () {
                }
            }, options);
            socket.emit("callMethod", {data: settings}, function (data) {
                origin !== undefined ? settings.callback.call(this, origin, data) : settings.callback.call(this, data)
            });
        }

        var observe = function (options, origin) {
            var settings = $.extend({
                expressions: [],
                cause: "AnimationChanged",
                trigger: function () {
                }
            }, options);
            $(document).bind("checkObserver_" + settings.cause, function () {
                socket.emit("observe", {data: settings}, function (data) {
                    origin !== undefined ? settings.trigger.call(this, origin, data) : settings.trigger.call(this, data)
                });
            });
        }
        // --------------------- Extend jQuery
        $.fn.executeEvent = function (options) {
            return this.click(function (e) {
                executeEvent(options, e.target)
            }).css('cursor', 'pointer')
        }

        $.fn.callMethod = function (options) {
            return this.click(function (e) {
                callMethod(options, e.target)
            }).css('cursor', 'pointer')
        }

        $.fn.observe = function (options) {
            observe(options, this)
            return this
        }
        // ---------------------

        // ---------------------
        // Return BMotion API functions
        // ---------------------
        return {
            socket: socket,
            callMethod: function (options, origin) {
                callMethod(options, origin)
            },
            executeEvent: function (options, origin) {
                executeEvent(options, origin)
            },
            observe: function (options, origin) {
                observe(options, origin)
            }
        }

    }
)

function fixSizeDialog(dialog, obj, ox, oy) {
    var newwidth = dialog.parent().width() - ox
    var newheight = dialog.parent().height() - oy
    obj.attr("style", "width:" + (newwidth) + "px;height:" + (newheight - 50) + "px");
}
