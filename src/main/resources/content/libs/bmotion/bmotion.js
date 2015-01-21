define(["jquery", "socketio", 'css!bmotion-css'], function () {

        // ---------------------
        // Establish client socket
        // ---------------------
        var socket = io.connect('http://localhost:9090');

        var observers = {};
        var formulaObservers = {};

        socket.on('checkObserver', function (trigger) {

            if (observers[trigger] !== undefined) {
                $.each(observers[trigger], function (i, v) {
                    v.call(this)
                });
            }

            if (formulaObservers[trigger] !== undefined) {
                socket.emit("observe", {data: formulaObservers[trigger]}, function (data) {
                    $.each(formulaObservers[trigger], function (i, v) {
                        v.observer.call(this, {values: data[i]})
                    });
                });
            }

        });

        socket.on('applyTransformers', function (data) {
            var d1 = JSON.parse(data);
            var i1 = 0;
            for (; i1 < d1.length; i1++) {
                var t = d1[i1];
                if (t.selector) {
                    var selector = $(t.selector);
                    var content = t.content;
                    if (content != undefined) selector.html(content);
                    selector.attr(t.attributes);
                    selector.css(t.styles)
                }
            }
        });

        var addObserver = function (cause, observer) {
            if (observers[cause] === undefined) observers[cause] = [];
            observers[cause].push(observer)
        };

        var addFormulaObserver = function (cause, settings, observer) {
            if (formulaObservers[cause] === undefined) formulaObservers[cause] = {};
            settings.observer = observer
            formulaObservers[cause][guid()] = settings
        };

        var guid = (function () {
            function s4() {
                return Math.floor((1 + Math.random()) * 0x10000)
                    .toString(16)
                    .substring(1);
            }

            return function () {
                return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
                    s4() + '-' + s4() + s4() + s4();
            };
        })();

        // ---------------------

        var executeEvent = function (options, origin) {
            var settings = normalize($.extend({
                events: [],
                callback: function () {
                }
            }, options), ["callback"], origin);
            socket.emit("executeEvent", {data: normalize(settings, ["callback"], origin)}, function (data) {
                origin !== undefined ? settings.callback.call(this, origin, data) : settings.callback.call(this, data)
            });
            return settings
        };

        var callMethod = function (options, origin) {
            var settings = normalize($.extend({
                name: "",
                callback: function () {
                }
            }, options), ["callback"], origin);
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
            addFormulaObserver(settings.cause, settings, function (data) {
                if (origin === undefined) {
                    settings.trigger.call(this, data)
                } else {
                    var el = settings.selector !== null ? $(settings.selector) : origin;
                    el.each(function (i, v) {
                        settings.trigger.call(this, $(v), data)
                    });
                }
            });
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
                observe(what, options, this);
                return this
            };

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
            callMethod: callMethod,
            executeEvent: executeEvent,
            observe: observe,
            addObserver: addObserver
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