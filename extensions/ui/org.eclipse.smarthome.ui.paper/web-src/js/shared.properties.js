'use strict';

angular.module('PaperUI.controllers').service('sharedProperties', function() {
    var triggersArray = [];
    var actionsArray = [];
    var conditionsArray = [];
    var params = [];
    var moduleTypes = [];
    return {
        updateParams : function(elem) {
            params.push(elem);
        },
        getParams : function() {
            return params;
        },
        resetParams : function() {
            params = [];
        },
        addArray : function(type, arr) {
            arr.type = type;
            var self = this;
            angular.forEach(arr, function(value) {
                if (self.searchVisibleType(arr, value.type) == -1) {
                    self.updateModule(arr.type, value);
                }
            });
        },
        updateModule : function(type, value) {
            var modArr = this.getModuleArray(type);
            var maxId = this.getMaxModuleId(modArr);
            if (!value.id) {
                value.id = type + "_" + maxId;
                modArr.push(value);
            } else {
                var index = this.searchArray(modArr, value.id);
                if (index != -1) {
                    modArr[index] = value;
                } else {
                    modArr.push(value);
                }
            }
        },
        getTriggersArray : function() {
            return triggersArray;
        },
        getActionsArray : function() {
            return actionsArray;
        },
        getConditionsArray : function() {
            return conditionsArray;
        },
        getModuleJSON : function(mtype) {
            var $moduleJSON = [];
            var i = 1;
            var modArr = this.getModuleArray(mtype);
            modArr.mtype = mtype;
            angular.forEach(modArr, function(value) {
                var type = typeof value.uid === "undefined" ? value.type : value.uid;
                $moduleJSON.push({
                    "id" : value.id,
                    "label" : value.label,
                    "description" : value.description,
                    "type" : type,
                    "configuration" : value.configuration ? value.configuration : {}
                });
                i++;
            });

            return $moduleJSON;
        },
        reset : function() {
            triggersArray = [];
            actionsArray = [];
            conditionsArray = [];
        },
        removeFromArray : function(opt, id) {
            var arr = null;
            if (angular.equals(opt, "trigger")) {
                arr = triggersArray;
            } else if (angular.equals(opt, "action")) {
                arr = actionsArray;
            } else if (angular.equals(opt, "condition")) {
                arr = conditionsArray;
            }

            var index = this.searchArray(arr, id);
            if (index != -1) {
                arr.splice(index, 1);
            }
        },
        searchArray : function(arr, uid) {

            var k;
            for (k = 0; arr != null && k < arr.length; k = k + 1) {
                if (arr[k].id === uid) {
                    return k;
                }
            }
            return -1;

        },

        searchVisibleType : function(arr, type) {

            var k;
            for (k = 0; arr != null && k < arr.length; k = k + 1) {
                if (arr[k].uid === type && arr[k].visibility.toUpperCase() === 'VISIBLE') {
                    return k;
                }
            }
            return -1;

        },

        getModuleArray : function(type) {
            if (type == 'trigger') {
                return triggersArray;
            } else if (type == 'action') {
                return actionsArray;
            } else if (type == 'condition') {
                return conditionsArray;
            }

        },

        setModuleTypes : function(mTypes) {
            moduleTypes = mTypes;
        },
        getMaxModuleId : function(modArr) {
            var max_id = 0;
            for (var i = 0; i < modArr.length; i++) {
                var id = modArr[i].id.split("_");
                if (id.length > 0 && !isNaN(parseInt(id[1])) && parseInt(id[1]) > max_id) {
                    max_id = parseInt(id[1]);
                }
            }
            return ++max_id;
        }
    }
});
angular.module('PaperUI.constants').constant('itemConfig', {
    'types' : [ 'Switch', 'Contact', 'String', 'Number', 'Dimmer', 'DateTime', 'Color', 'Image', 'Player', 'Location', 'Group' ],
    'groupTypes' : [ 'Switch', 'Contact', 'Number', 'Dimmer', 'None' ],
    'arithmeticFunctions' : [ {
        name : "AVG",
        value : "AVG"
    }, {
        name : "MAX",
        value : "MAX"
    }, {
        name : "MIN",
        value : "MIN"
    }, {
        name : "SUM",
        value : "SUM"
    } ],
    'logicalFunctions' : [ {
        name : "AND_ON_OFF",
        value : "All ON → ON else OFF"
    }, {
        name : "NAND_ON_OFF",
        value : "All ON → OFF else ON"
    }, {
        name : "OR_OFF_ON",
        value : "All OFF → OFF else ON"
    }, {
        name : "NOR_ON_OFF",
        value : "All OFF → ON else OFF"
    }, {
        name : "OR_ON_OFF",
        value : "One ON → ON else OFF"
    }, {
        name : "NOR_ON_OFF",
        value : "One ON → OFF else ON"
    }, {
        name : "AND_OFF_ON",
        value : "One OFF → OFF else ON"
    }, {
        name : "NAND_OFF_ON",
        value : "One OFF → ON else OFF"
    } ]
});