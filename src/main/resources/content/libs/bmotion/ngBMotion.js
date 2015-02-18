define(['bms', '/bms/libs/bmotion/config.js', 'angular-route', "bootstrap", "css!bootstrap-css"], function (bms, config) {

        var getChanges = function (prev, now) {
            var changes = {}, prop, pc;
            for (prop in now) {
                //if (!prev || prev[prop] !== now[prop]) {
                if (!prev || !prev.hasOwnProperty(prop)) {
                    if (typeof now[prop] == "object") {
                        if (c = getChanges(prev[prop], now[prop]))
                            changes[prop] = c;
                    } else {
                        changes[prop] = now[prop];
                    }
                }
            }
            for (prop in changes)
                return changes;
            return false; // false when unchanged
        };

        return angular.module('bmsModule', ['ngRoute'])
            .run(function ($rootScope) {
                $rootScope.formulaElements = [];
                $rootScope.addFormulaElement = function (element) {
                    $rootScope.formulaElements.push({value: $rootScope.formulaElements.length + 1, text: element})
                };
            })
            .factory('ws', ['$rootScope', function ($rootScope) {
                'use strict';
                return {
                    emit: function (event, data, callback) {
                        bms.socket.emit(event, data, function () {
                            var args = arguments;
                            $rootScope.$apply(function () {
                                if (callback) {
                                    callback.apply(null, args);
                                }
                            });
                        });
                    },
                    on: function (event, callback) {
                        bms.socket.on(event, function () {
                            var args = arguments;
                            $rootScope.$apply(function () {
                                callback.apply(null, args);
                            });
                        });
                    }
                };
            }])
            .factory('initSession', ['$q', 'ws', function ($q, ws) {
                var defer = $q.defer();
                // TODO: REMOVE DOM QUERY FROM CONTROLLER!!!
                var event = {
                    templateUrl: document.URL,
                    scriptPath: config.script == "" ? $("meta[name='bms.script']").attr("content") : config.script,
                    modelPath: config.model == "" ? $("meta[name='bms.model']").attr("content") : config.model,
                    tool: $("meta[name='bms.tool']").attr("content")
                };
                ws.emit('initSession', event, function (data) {
                    defer.resolve(data.standalone)
                });
                return defer.promise;
            }])
            .run(['$rootScope', 'initSession', function ($rootScope, initSession) {
            }])
            .directive('bmsApp', ['initSession', '$compile', function (initSession, $compile) {
                return {
                    priority: 1,
                    controller: function ($scope) {
                        $scope.modal = {
                            state: 'hide',
                            label: 'Loading visualisation ...',
                            setLabel: function (label) {
                                $scope.modal.label = label
                            },
                            show: function () {
                                $scope.modal.state = 'show'
                            },
                            hide: function () {
                                $scope.modal.state = 'hide'
                            }
                        }
                    },
                    link: function ($scope, element, attrs) {

                        var loadingModal1 = angular.element('<bms-loading-modal></bms-loading-modal>');
                        element.find("body").append($compile(loadingModal1)($scope));

                        $scope.modal.show();
                        $scope.modal.setLabel("Loading visualisation ...");

                        initSession.then(function () {
                            $scope.modal.hide()
                        })

                    }
                }
            }])
            .directive('bmsLoadingModal', function () {
                return {
                    restrict: 'E',
                    replace: true,
                    templateUrl: '/bms/libs/bmotion/bmsLoadingModal.html',
                    link: function ($scope, element, attrs) {
                        $scope.$watch('modal.state', function (nv) {
                            $(element).modal(nv)
                        })
                    }
                }
            })
            .controller('BMotionCtrl', ['$scope', 'ws', function ($scope, ws) {
            }])
            .directive("ngScopeElement", function () {
                var directiveDefinitionObject = {
                    restrict: "A",
                    compile: function compile(tElement, tAttrs, transclude) {
                        return {
                            pre: function preLink(scope, iElement, iAttrs, controller) {
                                scope[iAttrs.ngScopeElement] = iElement;
                            }
                        };
                    }
                };
                return directiveDefinitionObject;
            })
            .directive('bmsSvg', function () {
                return {
                    templateUrl: function (elem, attr) {
                        return attr.svg
                    },
                    link: function (scope, element, attrs) {
                        // scope.list is the jqlite element,
                        // scope.list[0] is the native dom element
                        var svg = scope.svg
                    }
                };
            })
            .directive('bmsVisualisation', ['$compile', function ($compile) {
                return {

                    //restrict: 'E',
                    //replace: true,
                    controller: function ($scope) {
                        $scope.values = [];
                        $scope.changes = [];
                        $scope.count = 0;
                        $scope.mapping = [];
                        $scope.order = [];
                        $scope.getValue = function (bmsid, attr, defaultValue) {
                            var returnValue = defaultValue === 'undefined' ? undefined : defaultValue;
                            if ($scope.values !== undefined) {
                                if ($scope.mapping[bmsid] !== undefined) {
                                    var lastIndex;
                                    $.each($scope.mapping[bmsid], function (i, v) {
                                        var index = $scope.order.indexOf(v);
                                        if ($scope.values[v] !== undefined && (index < lastIndex || lastIndex === undefined)) {
                                            lastIndex = index;
                                            returnValue = $scope.values[v][attr];
                                        }
                                    });
                                }
                            }
                            return returnValue;
                        };
                        $scope.setValues = function (values) {
                            $scope.values = values;
                        };
                        $scope.setOrder = function (order) {
                            $scope.order = order.reverse()
                        };
                    },
                    link: function (scope, element) {
                        scope.$watch('values', function (newValue, oldValue) {
                            var changes = getChanges(scope.changes, newValue);
                            $.extend(true, scope.changes, changes);
                            if (changes) {
                                $.each(changes, function (selector, attrs) {
                                    var orgElement = $(element.contents()).find(selector);
                                    $.each(attrs, function (attr, val) {
                                        orgElement.each(function () {
                                            var attrDefault = $(this).attr(attr);
                                            // Special case for class attributes
                                            if (attr === "class" && attrDefault === undefined) {
                                                attrDefault = ""
                                            }
                                            // Set internal bms id
                                            if (!$(this).attr("data-bms")) {
                                                $(this).attr("data-bms", "bms" + scope.count);
                                                scope.count++;
                                            }
                                            // Create mapping object, if not exists yet
                                            if (scope.mapping[$(this).attr("data-bms")] === undefined) {
                                                scope.mapping[$(this).attr("data-bms")] = [];
                                            }
                                            // Add selector to internal bms id map
                                            scope.mapping[$(this).attr("data-bms")].push(selector);
                                            // Initialise the getValue method
                                            $(this).attr("ng-attr-" + attr,
                                                "{{getValue('" + $(this).attr("data-bms") + "','" + attr + "','" + attrDefault + "')}}")
                                        });
                                    });
                                });
                                $compile(element.contents())(scope);
                            }
                        });
                    }
                }
            }]);

    }
);
