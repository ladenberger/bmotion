define(['bms', 'angular-route', "bootstrap", "css!bootstrap-css"], function (bms) {

        return angular.module('bmsModule', ['ngRoute'])
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
                    scriptPath: $("meta[name='bms.script']").attr("content"),
                    modelPath: $("meta[name='bms.model']").attr("content"),
                    tool: $("meta[name='bms.tool']").attr("content")
                };
                ws.emit('initSession', event, function (data) {
                    defer.resolve(data.standalone)
                })
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
                        var loadingModal1 = angular.element('<bms-loading-modal></bms-loading-modal>')
                        element.find("body").append($compile(loadingModal1)($scope))
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
            });

    }
)