$ = require('jquery');
const DDP = require("ddp.js").default;

function Smarti(options) {

    options = $.extend(true,{
        DDP:{
            SocketConstructor: WebSocket
        },
        pollingInterval:5000
    },options);

    var pubsubs = {};

    //taken from http://api.jquery.com/jQuery.Callbacks/
    function pubsub( id ) {
        var callbacks, method,
            pubsub = id && pubsubs[ id ];

        if ( !pubsub ) {
            callbacks = $.Callbacks();
            pubsub = {
                publish: callbacks.fire,
                subscribe: callbacks.add,
                unsubscribe: callbacks.remove
            };
            if ( id ) {
                pubsubs[ id ] = pubsub;
            }
        }
        return pubsub;
    }

    var ddp  = new DDP(options.DDP);

    function login(success,failure,username,password) {

        function loginRequest(params) {
            const loginId = ddp.method("login", params);
            ddp.on("result", function (message) {
                if (message.id == loginId) {

                    if (message.error) return failure({msg:message.error.reason});

                    //TODO fix
                    localStorage.setItem('Meteor.loginToken',message.result.token);
                    localStorage.setItem('Meteor.loginTokenExpires',new Date(message.result.tokenExpires.$date));
                    localStorage.setItem('Meteor.userId',message.result.id);

                    success();
                }
            });
        }

        if(username && password) {

            loginRequest([
                {
                    "user": {"username": username},
                    "password": password
                }, false
            ]);

        } else if(localStorage
            && localStorage.getItem('Meteor.loginToken')
            && localStorage.getItem('Meteor.loginTokenExpires')
            && (new Date(localStorage.getItem('Meteor.loginTokenExpires')) > new Date())) {

            console.log('found token %s for user %s that expires on %s',
                localStorage.getItem('Meteor.loginToken'),
                localStorage.getItem('Meteor.userId'),
                localStorage.getItem('Meteor.loginTokenExpires')
            );

            loginRequest([
                { "resume": localStorage.getItem('Meteor.loginToken') }
            ]);

        } else {
            failure({msg:'No auth token or token expired'});
        }
    }

    function load(success,failure) {
        success({some:'data'})
    }

    //TODO should be done with sockets in version II
    function poll() {
        /*$.ajax({
            url: options.smarti.pollingEndpoint,
            data: {channel:options.channel},
            success: function(data){
                //TODO constraints
                console.log(data);
                pubsub('smarti.data').publish(data)
            },
            dataType: "json",
            complete: function() {
                //TODO uncomment
                //setTimeout(poll,options.pollingInterval)
            },
            timeout: 30000
        });*/
    }

    poll();

    /*function connect(roomId, success, failure) {

        function listRooms() {
            const methodId = ddp.method("rooms/get");
            ddp.on("result", function(message) {
                if(message.id == methodId)
                    console.log(message);
                subscribe("GENERAL");
            });
        }

        function subscribe(roomid) {
            const subId = ddp.sub("stream-room-messages",[roomid,false]);console.log(subId);
            ddp.on("ready", function(message) {
                if (message.subs.includes(subId)) {
                    console.log(message);
                }
            });
        }

        ddp.on("added", function(message) {
            console.log(message);
        });

        ddp.on("changed", function(message) {
            console.log(message);
        });

        ddp.on("removed", function(message) {
            console.log(message);
        });

    }*/

    function postMessage(msg) {

    }

    function suggestMessage(msg) {

    }

    return {
        login: login,
        load: load,
        subscribe: function(id,func){pubsub(id).subscribe(func)},
        unsubscribe: function(id,func){pubsub(id).unsubscribe(func)},
        postMessage: postMessage,
        suggestMessage: suggestMessage
    }
}

function SmartiWidget(element,channel,config) {

    element = $(element);

    var mainDiv = $('<div>').addClass('widget').appendTo(element.empty());

    var contentDiv = $('<div>').appendTo(mainDiv);
    var messagesDiv = $('<div>').appendTo(mainDiv);

    var options = {
        socketEndpoint: "wss://rocket.redlink.io/websocket",
        pollingEndpoint: 'https://echo.jsontest.com/key/value',
        channel:channel || 'GENERAL'
    };

    var initialized = false;

    var smarti = Smarti({DDP:{endpoint:options.socketEndpoint},smarti:{pollingEndpoint:options.pollingEndpoint,channel:options.channel}});//TODO wait for connect?

    smarti.subscribe('smarti.data', function(data){
        if(initialized) drawWidget(data);
    });

    function showError(err) {
        messagesDiv.empty().append($('<p>').text(err.msg));
    }

    function drawLogin() {

        contentDiv.empty();

        var form = $('<form><span>Username</span><input type="text"><br><span>Password</span><input type="password"><br><button>Submit</button></form>');

        form.find('button').click(function(){

            var username = form.find('input[type="text"]').val();
            var password = form.find('input[type="password"]').val();

            smarti.login(
                initialize,
                showError,
                username,
                password
            );

            return false;
        });

        form.appendTo(contentDiv);

    }

    function drawWidget(data,success,failure) {

        contentDiv.empty();
        messagesDiv.empty();

        contentDiv.append("<p>Widget for "+channel+"</p>");

        if(success) success();
    }

    function initialize() {
        smarti.load(
            function(data) {
                drawWidget(data, function(){
                    initialized = true;
                })
            }
        )
    }

    smarti.login(
        initialize,
        drawLogin
    );

    function reload() {
        //TODO
    }

    return {
        reload:reload
    }
}

window.SmartiWidget = SmartiWidget;
