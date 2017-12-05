describe('module PaperUI.controllers.things', function() {
    beforeEach(function() {
        module('PaperUI');
    });

    describe('tests for ThingController', function() {
        var ThingController, scope, injector, deferred;
        beforeEach(inject(function($injector, $rootScope, $controller, $mdDialog, $q) {
            scope = $rootScope.$new();
            mdDialog = $mdDialog;
            $controller('BodyController', {
                '$scope' : scope
            });
            ThingController = $controller('ThingController', {
                '$scope' : scope
            });
            deferred = $q.defer();
        }));
        it('should require ThingController', function() {
            expect(ThingController).toBeDefined();
        });
        it('should open thing remove dialog', function() {
            spyOn(mdDialog, 'show').and.returnValue(deferred.promise);
            ;
            var event = {
                stopImmediatePropagation : function() {
                }
            };
            scope.remove(0, event);
            expect(mdDialog.show).toHaveBeenCalled();
        });
        it('should get thingType label', function() {
            scope.thingTypes = [ {
                label : '1'
            } ]
            var label = scope.getThingTypeLabel(0);
            expect(label).toEqual('1');
        });
    });

    describe('tests for ViewThingController', function() {
        var ViewThingController;
        var scope;
        var injector;
        var deferred;
        var channelTypeService;
        var itemRepository;
        beforeEach(inject(function($injector, $rootScope, $controller, $mdDialog, $q) {
            scope = $rootScope.$new();
            scope.path = [];
            scope.path[4] = 'TID';
            mdDialog = $mdDialog;
            channelTypeService = $injector.get('channelTypeService');
            itemRepository = $injector.get('itemRepository');
            spyOn(channelTypeService, 'getAll').and.callThrough();
            spyOn(itemRepository, 'getAll');

            var thingRepository = $injector.get('thingRepository');
            spyOn(thingRepository, "getOne");

            $controller('BodyController', {
                '$scope' : scope
            });
            ViewThingController = $controller('ViewThingController', {
                '$scope' : scope
            });
            deferred = $q.defer();
            injector = $injector;
        }));
        it('should require ViewThingController', function() {
            expect(ViewThingController).toBeDefined();
        });
        it('should get channelTypes and items', function() {
            expect(channelTypeService.getAll).toHaveBeenCalled();
            expect(itemRepository.getAll).toHaveBeenCalled();
        });
        it('should open remove thing dialog', function() {
            spyOn(mdDialog, 'show').and.returnValue(deferred.promise);
            var event = {
                stopImmediatePropagation : function() {
                }
            };
            scope.remove(0, event);
            expect(mdDialog.show).toHaveBeenCalled();
        });
        it('should link channel advance mode', function() {
            var event = {
                stopImmediatePropagation : function() {
                }
            };
            scope.thing = {};
            scope.thing.channels = [ {
                id : "T",
                linkedItems : [],
                channelTypeUID : 'C:T'
            } ];
            scope.channelTypes = [ {
                UID : 'C:T',
                category : ''
            } ];

            restConfig = injector.get('restConfig');
            $httpBackend = injector.get('$httpBackend');
            $httpBackend.when('GET', restConfig.restPath + "/channel-types/C:T/advicedProfile").respond({
                supportedItemTypes : [ "Switch", "Dimmer" ]
            });
            $httpBackend.expect('GET', restConfig.restPath + "/links/auto").respond();
            $httpBackend.expect('GET', restConfig.restPath + "/inbox").respond([]);
            $httpBackend.expect('GET', restConfig.restPath + "/bindings").respond();
            $httpBackend.expect('GET', restConfig.restPath + "/channel-types").respond();

            scope.advancedMode = true;

            spyOn(channelTypeService, 'getAdvicedProfile').and.callThrough();
            spyOn(mdDialog, 'show').and.returnValue(deferred.promise);

            scope.enableChannel(0, 'T', event, true);
            $httpBackend.flush();

            expect(channelTypeService.getAdvicedProfile).toHaveBeenCalledWith({
                channelTypeUID : "C:T"
            }, jasmine.any(Function));

            expect(mdDialog.show).toHaveBeenCalled();
        });
        it('should link channel simple mode', function() {
            var linkService = injector.get("linkService");
            var event = {
                stopImmediatePropagation : function() {
                }
            };
            scope.advancedMode = false;
            scope.thing = {
                UID : "C:T"
            };
            scope.thing.channels = [ {
                id : "T",
                linkedItems : []
            } ];
            spyOn(linkService, 'link');
            scope.enableChannel(0, 'T', event);
            expect(linkService.link).toHaveBeenCalled();
        });
        it('should unlink channel advance mode', function() {
            spyOn(mdDialog, 'show').and.returnValue(deferred.promise);

            var event = {
                stopImmediatePropagation : function() {
                }
            };
            scope.thing = {};
            scope.thing.channels = [ {
                id : "T",
                linkedItems : []
            } ];
            scope.channelTypes = [ {
                UID : 'C:T',
                category : ''
            } ];
            scope.advancedMode = true;
            scope.disableChannel(0, 'T', '', event);
            expect(mdDialog.show).toHaveBeenCalled();
        });
        it('should unlink channel simple mode', function() {
            var linkService = injector.get("linkService");
            var event = {
                stopImmediatePropagation : function() {
                }
            };
            scope.advancedMode = false;
            scope.thing = {
                UID : "C:T"
            };
            scope.thing.channels = [ {
                id : "T",
                linkedItems : []
            } ];
            spyOn(linkService, 'unlink');
            scope.disableChannel(0, 'T', '', event);
            expect(linkService.unlink).toHaveBeenCalled();
        });
    });
    describe('tests for RemoveThingDialogController', function() {
        var RemoveThingDialogController, scope, mdDialog, thingService;
        beforeEach(inject(function($injector, $rootScope, $controller, $mdDialog) {
            scope = $rootScope.$new();
            RemoveThingDialogController = $controller('RemoveThingDialogController', {
                '$scope' : scope,
                'thing' : {}
            });
            mdDialog = $mdDialog;
            thingService = $injector.get('thingService');
        }));
        it('should require ConfigurationPageController', function() {
            expect(RemoveThingDialogController).toBeDefined();
        });
        it('should require close dialog', function() {
            spyOn(mdDialog, 'cancel');
            scope.close();
            expect(mdDialog.cancel).toHaveBeenCalled();
        });
        it('should remove thing', function() {
            spyOn(thingService, 'remove');
            scope.remove(0);
            expect(thingService.remove).toHaveBeenCalled();
        });
    });
    describe('tests for LinkChannelDialogController', function() {
        var LinkChannelDialogController, scope, itemService, deferred;
        beforeEach(inject(function($injector, $rootScope, $controller, $mdDialog, $q) {
            scope = $rootScope.$new();
            $rootScope.data.items = [ {
                type : 'T'
            } ];
            var itemRepository = $injector.get('itemRepository');
            spyOn(itemRepository, 'getAll').and.callFake(function(callback) {
                return callback([ {
                    type : 'T'
                } ]);
            });
            LinkChannelDialogController = $controller('LinkChannelDialogController', {
                '$scope' : scope,
                'params' : {
                    'linkedItems' : [],
                    'acceptedItemType' : 'T',
                    'category' : ''
                }
            });
            mdDialog = $mdDialog;
            deferred = $q.defer();
            itemService = $injector.get('itemService');
        }));
        it('should require LinkChannelDialogController', function() {
            expect(LinkChannelDialogController).toBeDefined();
        });
        it('should fetch items', function() {
            expect(scope.items.length).toEqual(1);
            expect(scope.itemsList.length).toEqual(2);
        });
        it('should toggle items form', function() {
            scope.checkCreateOption();
            expect(scope.itemFormVisible).toBeFalsy();
            scope.itemName = "_createNew";
            scope.checkCreateOption();
            expect(scope.itemFormVisible).toBeTruthy();
        });
        it('should create item and link', function() {
            spyOn(itemService, 'create').and.returnValue(deferred.promise).and.callThrough();
            spyOn(itemService.create({}).$promise, 'then').and.returnValue(deferred.promise);
            scope.newItemName = "N";
            scope.createAndLink();
            expect(itemService.create).toHaveBeenCalled();
        });
    });
});