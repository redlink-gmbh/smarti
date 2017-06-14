require('./style.scss');
$ = require('jquery');
const DDP = require("ddp.js").default;

function Smarti(options) {

    var conversationId = undefined;

    var lastupdate = undefined;

    options = $.extend(true,{
        DDP:{
            SocketConstructor: WebSocket
        },
        pollingInterval:5000
    },options);

    var pubsubs = {};

    //init socket connection
    var ddp  = new DDP(options.DDP);

    /**
     * Enabled publication-subscription mechanism
     * @param id
     * @returns {*}
     */
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

    /**
     * Login user for socket communication. If username AND password is provided, the system tries to login with this credentials.
     * If not, the system checks local storage for user tokens and (if token is not expired) logs in the user.
     * @param success method that is called on success
     * @param failure method that is called on failure
     * @param username the username
     * @param password the password
     */
    function login(success,failure,username,password) {

        /**
         * Login to meteor ddp sockets with params
         * @param params
         */
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

    function init(success,failure) {
        //get conversation id
        $.ajax({
                url: options.smarti.endpoint +
                    'rocket/' +
                    'rocket.redlink.io/' + //TODO hard coded!
                    options.channel +
                    '/conversationid',
                type: 'GET',
                dataType: 'text'
            }).done(function(data) {
                conversationId = data;
                success(data);
            }).fail(function(err){
                console.log(err);
                failure(err);
            });
    }

    //TODO should be done with sockets in version II
    function poll() {
        $.ajax({
            url: options.smarti.endpoint + 'conversation/' + conversationId,
            success: function(data){
                if(!lastupdate || lastupdate < data.lastModified) {
                    console.log(data);
                    lastupdate = data.lastModified;
                    pubsub('smarti.data').publish(data)
                }
            },
            dataType: "json",
            complete: function() {
                setTimeout(poll,options.pollingInterval)
            },
            timeout: options.pollingInterval+1 //TODO check
        });
    }

    function query(template,creator,success,failure) {
        $.getJSON(options.smarti.endpoint + 'conversation/' + conversationId + '/template/' + template + '/' + creator,
            function(data){
                success(data);
            }, function(err) {
                failure({msg:err});
            }
        )
    }

    function postMessage(msg,attachments,success,error) {
        const methodId = ddp.method("sendMessage",[{rid:options.channel,msg:msg,attachments:attachments}]);
        ddp.on("result", function(message) {
            if(message.id == methodId) {
                if(message.error && error) error(message.error);
                else if(success) success();
            }
        });
    }

    function suggestMessage(msg) {

    }

    return {
        login: login,
        init: init,
        poll: poll,
        subscribe: function(id,func){pubsub(id).subscribe(func)},
        unsubscribe: function(id,func){pubsub(id).unsubscribe(func)},
        query: query,
        postMessage: postMessage,
        suggestMessage: suggestMessage
    }
}

function SmartiWidget(element,channel,config) {

    var initialized = false;

    var options = {
        socketEndpoint: "wss://rocket.redlink.io/websocket/", //TODO make configurable
        smartiEndpoint: 'https://dev.cerbot.redlink.io/9501/',
        rocketBaseurl: '/',
        channel:channel || 'GENERAL'
    };

    var widgets = {
        solr:[],
        conversations:[]
    };

    function SolrWidget(elem,slots,tempid,tokens,query) {

        elem.append('<h2>' + query.displayTitle + '</h2>');
        var content = $('<div>').appendTo(elem);

        function createTermPill(token) {
            return $('<div class="smarti-token-pill">')
                .append($('<span>').text(token.value))
                .append('<i class="icon-cancel"></i>')
                .data('token',token)
                .click(function(){
                    $(this).hide();
                    getResults();
                });
        }

        //TODO should be done server side
        function removeDuplicatesBy(keyFn, array) {
            var mySet = new Set();
            return array.filter(function(x) {
                var key = keyFn(x), isNew = !mySet.has(key);
                if (isNew) mySet.add(key);
                return isNew;
            });
        }

        var termPills = $('<div class="smarti-token-pills">').appendTo(content);


        function perparePillTokens(slots,tokens) {
            var pillTokens = [];
            $.each(slots,function(i,slot){
                if(slot.tokenIndex != undefined && slot.tokenIndex > -1) {
                    pillTokens.push(tokens[slot.tokenIndex]);
                } else if(!slot.tokenIndex) {
                    pillTokens.push(slot.token);
                }
            });
            return pillTokens;
        }


        var pillTokens = perparePillTokens(slots,tokens);

        $.each(removeDuplicatesBy(function(v){return v.value},pillTokens), function(i,t){
            termPills.append(createTermPill(t));
        });

        function refresh(data) {
            var tokens = data.tokens;
            var slots = data.templates[tempid].slots;
            console.log('refresh SolrW');
            var pillTokens = perparePillTokens(slots,tokens);

            var reload = false;

            $.each(removeDuplicatesBy(function(v){return v.value},pillTokens), function(i,t){
                var contained = false;
                $.each(termPills.children(), function(j,tp){
                    if($(tp).data('token').value == t.value) {
                        contained = true;
                    }
                });
                if(!contained) {
                    termPills.append(createTermPill(t));
                    reload = true;
                }
            });

            if(reload) {
                getResults();
            }
        }

        var inputForm = $('<div class="search-form" role="form"><div class="input-line search"><input type="text" class="search content-background-color" placeholder="Weiter Suchterme" autocomplete="off"> <i class="icon-search secondary-font-color"></i> </div></div>');
        var inputField = inputForm.find('input');

        inputField.keypress(function(e){
            if(e.which == 13) {
                var val = $(this).val();
                if(val!= undefined && val != "") {
                    termPills.append(createTermPill({
                        origin:'User',
                        value:val,
                        type:'Keyword'
                    }));
                    $(this).val("");
                }
                getResults();
            }
        });

        elem.append(inputForm);
        var resultCount = $('<h3></h3>').appendTo(elem);
        var loader = $('<div class="loading-animation"> <div class="bounce1"></div> <div class="bounce2"></div> <div class="bounce3"></div> </div>').hide().appendTo(elem);
        var results = $('<ul class="search-results">').appendTo(elem);

        function getResults() {
            var tks = termPills.children(':visible').map(function(){return $(this).data().token.value}).get().join(" ");
            //TODO get query string from remote
            query.url = 'https://dev.cerbot.redlink.io/9502/solr/main/search?wt=json&q=' + tks;

            results.empty();
            resultCount.empty();
            loader.show();
            $.ajax({
                url: query.url,
                success: function(data){ console.log(data);
                    loader.hide();
                    //hacky
                    var docs = $.map(data.response.docs.slice(0,3), function(doc) {
                        /*return {
                            source: doc.dbsearch_source_name_s + '/' + doc.dbsearch_space_name_t,
                            title: doc.dbsearch_title_s,
                            description: doc.dbsearch_excerpt_s,
                            type: doc.dbsearch_doctype_s,
                            doctype: doc.dbsearch_content_type_aggregated_s.slice(0,4),
                            link: doc.dbsearch_link_s,
                            date: new Date(doc.dbsearch_pub_date_tdt)
                        }*/
                        return {
                            source: doc.source,
                            title: doc.title,
                            description: doc.description,
                            type: doc.type,
                            doctype: doc.type.substring(doc.type.indexOf('/')+1).substring(doc.type.indexOf('+')).slice(0,4),
                            link: doc.url,
                            date: new Date(),
                            thumb: doc.thumbnail ? 'https://dev.cerbot.redlink.io/9502/solr/main/tn/' + doc.thumbnail : undefined
                        }
                    });

                    resultCount.text('Top 3 von ' + data.response.numFound);

                    $.each(docs,function(i,doc){
                        var docli = $('<li>' +
                            (doc.thumb ? '<div class="result-type"><div class="result-avatar-image" style="background-image:url(\''+doc.thumb+'\')"></div></div>' : '<div class="result-type result-type-'+doc.doctype+'"><div>'+doc.doctype+'</div></div>') +
                            '<div class="result-content"><div class="result-content-title"><a href="'+doc.link+'" target="blank">'+doc.title+'</a><span>'+doc.date.toLocaleDateString()+'</span></div>' + (doc.description ? '<p>'+doc.description+'</p>' : '') + '</div>' +
                            '<div class="result-actions"><button class="postAnswer">Posten<i class="icon-paper-plane"></i></button></div>'+
                            (i+1 != docs.length ? '<li class="result-separator"><div></div></li>':'') +
                            '</li>');

                        docli.find('.postAnswer').click(function(){
                            var text = "Das habe ich dazu in " + query.displayTitle + " gefunden.";
                            var attachments = [{
                                title: doc.title,
                                title_link: doc.link,
                                thumb_url: doc.thumb ? doc.thumb : 'http://www.s-bahn-berlin.de/img/logo-db.png',//TODO should be per creator
                                text:doc.description
                            }];
                            smarti.postMessage(text,attachments);
                        });

                        results.append(docli);
                    })
                },
                dataType: "json",
                failure: function(err) {
                    console.error(err);
                }
            });
        }

        getResults();

        return {
            refresh: refresh
        }
    }

    function ConversationWidget(elem,slots,tempid,tokens,query) {

        elem.append('<h2>' + query.displayTitle + '</h2>');

        function refresh() {
            console.log('refresh Conversatrion W');
        }

        var loader = $('<div class="loading-animation"> <div class="bounce1"></div> <div class="bounce2"></div> <div class="bounce3"></div> </div>').hide().appendTo(elem);
        var results = $('<ul class="search-results">').appendTo(elem);

        function getResults() {
            //TODO get remote
            results.empty();
            loader.show();

            smarti.query(tempid,query.creator,function(data){
            //$.getJSON('https://dev.cerbot.redlink.io/9503/data/conversations.json', function(data){
                console.log(data);
                loader.hide();

                function buildLink(msgid) {
                    return "http://permalink.org?msg="+msgid;
                }

                function buildAttachments(doc) {
                    var attachment = {
                        author_name: doc.userName,
                        author_link: buildLink(doc.messageId),
                        author_icon: options.rocketBaseurl + 'avatar/' + doc.userName,
                        //thumb_url: doc.img ? doc.img : undefined,
                        text: doc.content,
                        attachments: [],
                        bot: 'assistify',
                        ts:doc.timestamp
                    };

                    $.each(doc.answers, function(i,answer) {
                        attachment.attachments.push(buildAttachments(answer));
                    });

                    return attachment;
                }

                $.each(data, function (i, doc) {
                    //doc.img = 'https://pbs.twimg.com/profile_images/847117309965733888/dTaDJEjv.png';

                    function getSubcontent(docs) {
                        var result = '<ul>';

                        $.each(docs, function(j,subdoc){
                            result += '<li><div class="subdoc-title"><img src="'+options.rocketBaseurl + 'avatar/'+subdoc.userName+'">' +
                            '<a href="'+buildLink(subdoc.messageId)+'">'+subdoc.userName+'</a><span>'+(new Date(subdoc.timestamp)).toLocaleDateString()+'</span>' +
                            '<div class="subdoc-content">'+subdoc.content+'</div></li>'
                        });

                        return result + '</ul>'
                    }

                    var docli = $('<li>' +
                        '<div class="result-type"><div class="result-avatar-image" style="background-image:url(\''+options.rocketBaseurl + 'avatar/'+doc.userName+'\')"></div></div>' +
                        '<div class="result-content"><div class="result-content-title"><a href="' + buildLink(doc.messageId) + '" target="blank">' + doc.userName + '</a><span>' + (new Date(doc.timestamp)).toLocaleString() + '</span></div><p>' + doc.content + '</p></div>' +
                        '<div class="result-subcontent">'+ getSubcontent(doc.answers) + '</div>'+
                        '<div class="result-actions"><button class="postAnswer">Posten<i class="icon-paper-plane"></i></button></div>' +
                        (i + 1 != data.length ? '<li class="result-separator"><div></div></li>' : '') +
                        '</li>');

                    docli.find('.postAnswer').click(function () {
                        var text = "Ich habe eine passende Konversation gefunden.";
                        var attachments = [buildAttachments(doc)];
                        smarti.postMessage(text, attachments);
                    });

                    results.append(docli);
                })

            });
        }

        getResults();

        return {
            refresh: refresh
        }
    }

    element = $(element);

    $('<div class="title"> <h2>Smarti</h2> </div>').appendTo(element.empty());
    var mainDiv = $('<div>').addClass('widget').appendTo(element);


    var contentDiv = $('<div>').appendTo(mainDiv);
    var messagesDiv = $('<div>').appendTo(mainDiv);

    var smarti = Smarti({DDP:{endpoint:options.socketEndpoint},smarti:{endpoint:options.smartiEndpoint},channel:options.channel,rocket:{endpoint:options.rocketBaseurl}});//TODO wait for connect?

    smarti.subscribe('smarti.data', function(data){
        refreshWidgets(data);
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

    function refreshWidgets(data) {
        if(!initialized) {
            contentDiv.empty();
            messagesDiv.empty();

            $.each(data.templates, function(i, template){
                $.each(template.queries, function(j, query) {
                    switch(template.type) {
                        case 'dbsearch': widgets.solr.push(new SolrWidget($('<div class="smarti-widget">').appendTo(contentDiv),template.slots,i,data.tokens,query));break;
                        case 'related.conversation': widgets.solr.push(new ConversationWidget($('<div class="smarti-widget">').appendTo(contentDiv),template.slots,i,data.tokens,query));break;
                    }
                })
            });

            initialized = true;
        } else {
            $.each(widgets, function(i,wgts){
                $.each(wgts,function(j,wgt){
                    wgt.refresh(data);//TODO with data
                })
            })
        }
    }

    function initialize() {
        smarti.init(
            smarti.poll, //TODO should be removed
            showError
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
    };
}

window.SmartiWidget = SmartiWidget;
