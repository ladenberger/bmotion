define(["jquery", "socketio", 'css!bmotion-css'], function () {

        // ---------------------
        // Establish client socket
        // ---------------------
        var socket = io.connect('http://localhost:9090');

        var observers = {};

        socket.on('checkObserver', function (trigger) {
            if (observers[trigger] !== undefined) {
                $.each(observers[trigger], function (i, v) {
                    v.call(this)
                });
            }
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

        var addObserver = function (cause, observer) {
            if (observers[cause] === undefined) observers[cause] = [];
            observers[cause].push(observer)
        };

        // ---------------------

        var executeEvent = function (options, origin) {
            var settings = $.extend({
                events: [],
                callback: function () {
                }
            }, options);
            socket.emit("executeEvent", {data: normalize(settings, ["callback"], origin)}, function (data) {
                origin !== undefined ? settings.callback.call(this, origin, data) : settings.callback.call(this, data)
            });
            return settings
        };

        var callMethod = function (options, origin) {
            var settings = $.extend({
                name: "",
                callback: function () {
                }
            }, options);
            socket.emit("callMethod", {data: normalize(settings, ["callback"], origin)}, function (data) {
                origin !== undefined ? settings.callback.call(this, origin, data) : settings.callback.call(this, data)
            });
            return settings
        };

        var observeMethod = function (options, origin) {
            var settings = normalize($.extend({
                selector: null,
                name: "",
                cause: "AnimationChanged",
                trigger: function () {
                }
            }, options), ["trigger"], origin);
            addObserver(settings.cause, function () {
                socket.emit("callMethod", {data: settings}, function (data) {
                    var el = settings.selector !== null ? $(settings.selector) : origin;
                    el !== undefined ? settings.trigger.call(this, el, data) : settings.trigger.call(this, data)
                });
            });
            return settings
        };

        var observeFormulas = function (options, origin) {
            var settings = normalize($.extend({
                selector: null,
                formulas: [],
                cause: "AnimationChanged",
                trigger: function () {
                }
            }, options), ["trigger"], origin);
            addObserver(settings.cause, function () {
                socket.emit("observe", {data: settings}, function (data) {
                    if (origin === undefined) {
                        settings.trigger.call(this, data)
                    } else {
                        var el = settings.selector !== null ? $(settings.selector) : origin;
                        el.each(function (i, v) {
                            settings.trigger.call(this, $(v), data)
                        });
                    }
                });
            });
            /*$(document).bind("checkObserver_" + settings.cause, function () {
             socket.emit("observe", {data: settings}, function (data) {
             var el = settings.selector !== null ? $(settings.selector) : origin
             el !== undefined ? settings.trigger.call(this, el, data) : settings.trigger.call(this, data)
             });
             });*/
            return settings
        };

        var observe = function (what, options, origin) {
            if (what === "formula") {
                return observeFormulas(options, origin)
            }
            if (what === "method") {
                return observeMethod(options, origin)
            }
        };

        // ---------------------
        // jQuery extension
        // ---------------------
        (function ($) {

            $.fn.observe = function (what, options) {
                observe(what, options, this)
                return this
            }

            $.fn.executeEvent = function (options) {
                return this.click(function (e) {
                    executeEvent(options, e.target)
                }).css('cursor', 'pointer')
            }

        }(jQuery));

        // ---------------------
        // Return BMotion API functions
        // ---------------------
        return {
            socket: socket,
            callMethod: function (options, origin) {
                return callMethod(options, origin)
            },
            executeEvent: function (options, origin) {
                return executeEvent(options, origin)
            },
            observe: function (what, options, origin) {
                return observe(what, options, origin)
            },
            addObserver: function (cause, observer) {
                addObserver(cause, observer);
            }
        }

    }
);

function normalize(obj, exclude, origin) {
    exclude = exclude === 'undefined' ? [] : exclude;
    var obj2 = $.extend(true, {}, obj);
    _normalize(obj2, exclude, origin);
    return obj2
}

function _normalize(obj, exclude, origin) {
    for (var property in obj) {
        if (obj.hasOwnProperty(property)) {
            if (typeof obj[property] == "object") {
                _normalize(obj[property], exclude, origin);
            } else {
                if ($.inArray(property, exclude) === -1) {
                    if (isFunction(obj[property])) {
                        var r = origin !== undefined ? obj[property](origin) : obj[property]();
                        obj[property] = r
                    }
                }
            }
        }
    }
}

function isFunction(functionToCheck) {
    var getType = {};
    return functionToCheck && getType.toString.call(functionToCheck) === '[object Function]';
}