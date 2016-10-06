var mockPlayRoutes;
mockPlayRoutes = {
    'controllers' : {
        'Users' : {
            currentLoggedInUser : function() {
                return createMockPromise();
            }
        }
    }
};

var createMockPromise = function() {
    var getFunction = {};
    getFunction.get = function() {
        var promise = {};
        promise.then = function(f) {
            f({}); // call function with object
        };
        promise.success = function(f) {
            f({}); // call function with object
        };
        promise['finally'] = function(f) {
            f();
        };
        return promise;
    }
    return getFunction;
};