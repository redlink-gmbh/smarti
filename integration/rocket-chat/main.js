/*
 * Copyright 2017 Redlink GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

require('./style.scss');
$ = require('jquery');
md5 = require('js-md5');

const ld_lang = require('lodash/lang');

const DDP = require("ddp.js").default;

//multi-linguality
const Localize = require('localize');
const localize = new Localize({
    "login.no-auth-token": {
        "en": "No auth-token or token expired",
        "de": "Kein auth-token oder token abgelaufen."
    },
    "login.failed": {
        "de":"Anmeldung fehlgeschlagen: $[1]",
        "en":"Login failed: $[1]"
    },
    "sub.new-conversation-result.nosub": {
        "en": "Subscription to stream 'new-conversation-result' failed",
        "de": "Anmeldung an den stream 'new-conversation-result' fehlgeschlagen"
    },
    "smarti.result.no-result-yet": {
        "de": "Noch keine Antworten verfügbar",
        "en": "No answers yet"
    },
    "msg.post.failure": {
        "de": "Nachricht konnte nicht gepostet werden",
        "en": "Posting message failed"
    },
    "get.conversation.params": {
        "de": "Konversations-Parameter konnten nicht geladen werden: $[1]",
        "en": "Cannot load conversation params: $[1]"
    },
    "widget.latch.query.failed":{
        "de": "Widget $[1] hat Probleme bei der Anfrage: $[2]",
        "en": "Widget $[1] has problems while quering: $[2]"
    },
    "widget.latch.query.no-results":{
        "de":"Keine Ergbenisse",
        "en":"No results"
    },
    "widget.latch.query.header":{
        "en": "$[1] results",
        "de": "$[1] Ergebnisse"
    },
    "widget.latch.query.header.paged":{
        "en": "Page $[1] of $[2] results",
        "de": "Seite $[1] von $[2] Ergebnissens"
    },
    "widget.latch.query.paging.next":{
        "en": "Next",
        "de": "Nächste"
    },
    "widget.latch.query.paging.prev":{
        "en": "Previous",
        "de": "Vorherige"
    },
    "widget.latch.answer.title":{
        "de": "Das hab ich dazu in $[1] gefunden:",
        "en": "That I found in $[1]:"
    },
    "widget.conversation.title":{
        "en":"Related Conversation",
        "de":"Ähnliche Konversationen"
    },
    "widget.conversation.no-results":{
        "en":"No related Conversation",
        "de":"Keine ähnlichen Konversationen"
    },
    "widget.conversation.answer.title": {
        "de":"Ich habe eine passende Konversation gefunden:",
        "en":"I found a similar conversation:"
    },
    "widget.conversation.answer.title_msg": {
        "de":"Ich habe eine passende Nachricht gefunden:",
        "en":"I found a related message:"
    },
    "widget.conversation.post-all": {
        "de":"Alle $[1] Nachrichten posten",
        "en":"Post all $[1] messages"
    }
});

const Utils = {
    getAvatarUrl : function(id) {
        return "https://www.gravatar.com/avatar/"+md5('redlink'+id)+"?d=identicon"
    },
    getAnonymUser: function(id) {
        return 'User-' + (parseInt(md5('redlink'+id),16)%10000); //TODO check if this works somehow...
    },
    localize: function(obj) {
        if(obj.args && obj.args.length > 0) {
            var args_clone = ld_lang.cloneDeep(obj.args);
            args_clone.unshift(obj.code);
            return localize.translate.apply(null,args_clone);
        }
        return localize.translate(obj.code);
    },
    mapDocType: function(doctype) {
        switch(doctype) {
            case 'application/xhtml+xml': return 'html';
        }
        return doctype.substring(doctype.indexOf('/')+1).slice(0,4);
    }
};

/**
 * Manages the interaction between Plugin and RocketChat/Smarti
 * @param options: {
 *      DDP: {
 *          endpoint: STRING
 *      },
 *      smarti: {
 *          endpoint: STRING
 *      },
 *      channel: STRING,
 *      rocket: {
 *          endpoint: STRING
 *      },
 *      tracker: Tracker
 * }
 * @returns {
 *      login: function(success,failure,username,password),
 *      init: function(success, failure),
 *      subscribe: function(id,handler),
 *      unsubscribe: function(id,handler)
 *      query: function(params,success,failure)
 *      post: function(msg,attachments,success,failure)
 *      suggest: function(msg,success,failure)
 * }
 */
function Smarti(options) {

    options = $.extend(true,{
        DDP:{
            SocketConstructor: WebSocket
        },
        tracker: new Tracker()
    },options);

    //init socket connection
    var ddp  = new DDP(options.DDP);

    var pubsubs = {};
    /**
     * Enabled publication-subscription mechanism
     * @param id
     * @returns {*}
     */
    //taken from http://api.jquery.com/jQuery.Callbacks/
    function pubsub( id ) {
        var callbacks, pubsub = id && pubsubs[ id ];

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

                    if (message.error) return failure({code:"login.failed",args:[message.error.reason]});

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

            console.debug('found token %s for user %s that expires on %s',
                localStorage.getItem('Meteor.loginToken'),
                localStorage.getItem('Meteor.userId'),
                localStorage.getItem('Meteor.loginTokenExpires')
            );

            loginRequest([
                { "resume": localStorage.getItem('Meteor.loginToken') }
            ]);

        } else {
            failure({code:'login.no-auth-token'});
        }
    }

    /**
     *
     * @param failure
     */
    function init(success, failure) {

        //subscribe
        const subId = ddp.sub("stream-notify-room",[options.channel+"/newConversationResult",false]);

        ddp.on("nosub", function(message) {
            failure({code:'sub.new-conversation-result.nosub'});
        });

        ddp.on("changed", function(message) {
            if(message.id = subId) {
                fetch(message.fields.args[0]);
            }
        });

        //get last conversation result
        const lastConfCallId = ddp.method("getLastSmartiResult",[options.channel]);
        ddp.on("result", function(message) {

            if (message.error) return failure({code:"get.conversation.params",args:[message.error.reason]});

            if(message.id == lastConfCallId) {
                if(message.result) {
                    fetch(message.result);
                    if(success) success();
                } else {
                    if(failure) failure({code:'smarti.result.no-result-yet'});
                }
            }
        });

    }

    function fetch(params) {

        console.debug('fetch results for %s with token %s', params.conversationId, params.token);

        $.ajax({
            url: options.smarti.endpoint + 'conversation/' + params.conversationId,
            success: function(data){

                console.debug('received conversation results for %s:\n%s', params.conversationId, JSON.stringify(data,null,2));

                pubsub('smarti.data').publish(data);
            },
            failure: function(err) {
                console.error('conversation does not exist on smarti:',JSON.stringify(err,null,2));
            },
            dataType: "json"
        });

    }

    function query(params,success,failure) {
        $.getJSON(options.smarti.endpoint + 'conversation/' + params.conversationId + '/template/' + params.template + '/' + params.creator,
            function(data){
                success(data);
            }, function(err) {

                console.error('cannot get query results for params:\n%s\n,');

                failure({msg:err});
            }
        )
    }

    function post(msg,attachments,success,failure) {
        const methodId = ddp.method("sendMessage",[{rid:options.channel,msg:msg,attachments:attachments,origin:'smartiWidget'}]);

        ddp.on("result", function(message) {
            if(message.id == methodId) {
                if(message.error && error) {

                    console.debug('cannot post message:\n%s', JSON.stringify(message.error,null,2));

                    if(failure) failure({code:"msg.post.failure"});
                }
                else if(success) success();
            }
        });
    }

    return {
        login: login,
        init: init,
        subscribe: function(id,func){pubsub(id).subscribe(func)},
        unsubscribe: function(id,func){pubsub(id).unsubscribe(func)},
        query: query,
        post: post
    }
}

/**
 * A tracker wrapper
 * @param category
 * @param onEvent the real tracker methode which is called
 * @constructor
 */
function Tracker(category, roomId, onEvent) {
    this.trackEvent = function(action, value) {
        console.debug(`track event: ${category}, ${action}, ${roomId}, ${value}`);
        if(onEvent) onEvent(category, action, roomId, value);
    }

}

/**
 * Generates a smarti widget and appends it to element
 * @param element a dom element
 * @param config: {
 *       socketEndpoint: "ws://localhost:3000/websocket/",
 *       smartiEndpoint: 'http://localhost:8080/',
 *       channel: 'GENERAL',
 *       widget:{
 *       },
 *       lang:'de',
 *       inputFieldSelector: '.selector'
 *   }
 * @returns {
 *
 * }
 * @constructor
 */
function SmartiWidget(element,_options) {

    var initialized = false;

    var options = {
        socketEndpoint: "ws://localhost:3000/websocket/",
        smartiEndpoint: 'http://localhost:8080/',
        channel: 'GENERAL',
        postings: {
            type: 'suggestText', // possible values: suggestText, postText, postRichText
            cssInputSelector: '.message-form-text.input-message'
        },
        widget: {
        },
        tracker: {
            onEvent: (typeof Piwik != 'undefined' && Piwik) ? Piwik.getTracker().trackEvent : function(){},
            category: "knowledgebase"
        },
        lang: 'de'
    };

    $.extend(true,options,_options);

    console.debug('init smarti widget:\n%s', JSON.stringify(options,null,2));

    localize.setLocale(options.lang);

    var tracker = new Tracker(options.tracker.category,options.channel,options.tracker.onEvent);

    var widgets = [];

    var messageInputField = undefined;

    function InputField(elem) {
        this.post = function(msg) {
            console.debug(`write text to element: ${msg}`);
            elem.val(msg);
            elem.focus();
        }
    }

    if(options.postings && options.postings.type == 'suggestText') {
        if(options.postings.cssInputSelector) {
            var inputFieldELement = $(options.postings.cssInputSelector);
            if(inputFieldELement.length) {
                messageInputField = new InputField(inputFieldELement);
            } else {
                console.warn('no element found for cssInputSelector %s, set postings.type to postText',options.postings.inputFieldSelector);
                options.postings.type = 'postText';
            }
        } else {
            console.warn('No cssInputSelector set, set postings.type to postText');
            options.postings.type = 'postText';
        }
    }

    /**
     * @param params
     * @param wgt_conf
     * @returns {
     *      refresh: FUNCTION
     * }
     * @constructor
     */
    function IrLatchWidget(params,wgt_conf) {

        const numOfRows = wgt_conf.numOfRows || params.query.resultConfig.numOfRows;

        params.elem.append('<h2>' + params.query.displayTitle + '</h2>');
        var content = $('<div>').appendTo(params.elem);

        function createTermPill(token) {

            return $('<div class="smarti-token-pill">')
                .append($('<span>').text(token.value))
                .append('<i class="icon-cancel"></i>')
                .data('token',token)
                .click(function(){
                    $(this).hide();
                    getResults(0);
                    tracker.trackEvent(params.query.creator + ".tag.remove");
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


        var pillTokens = perparePillTokens(params.slots,params.tokens);

        $.each(removeDuplicatesBy(function(v){return v.value},pillTokens), function(i,t){
            termPills.append(createTermPill(t));
        });

        function refresh(data) {

            console.debug('refresh ir-latch search widget:\n%s', JSON.stringify(data,null,2));

            var tokens = data.tokens;
            var slots = data.templates[params.tempid].slots;
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
                getResults(0);
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
                    tracker.trackEvent(params.query.creator + ".tag.add");
                }
                getResults(0);
            }
        });

        params.elem.append(inputForm);
        var resultCount = $('<h3></h3>').appendTo(params.elem);
        var loader = $('<div class="loading-animation"> <div class="bounce1"></div> <div class="bounce2"></div> <div class="bounce3"></div> </div>').hide().appendTo(params.elem);
        var results = $('<ul class="search-results">').appendTo(params.elem);
        var resultPaging = $('<table>').addClass('paging').appendTo(params.elem);

        function getResults(page) {
            var tks = termPills.children(':visible').map(function(){return $(this).data().token.value}).get().join(" ");

            //TODO still a hack !!!
            params.query.url = params.query.url.substring(0,params.query.url.indexOf('?')) + '?wt=json&fl=*,score&rows=' + numOfRows + '&q=' + tks;
            if (wgt_conf.suffix) {
                params.query.url += wgt_conf.suffix;
            }

            //append params
            for(var property in params.query.defaults) {
                params.query.url += '&' + property + "=" + params.query.defaults[property];
            }

            if(page > 0) {
                //append paging
                params.query.url += '&start=' + (page*numOfRows);
            }

            results.empty();
            resultCount.empty();
            resultPaging.empty();
            loader.show();

            console.log(`executeSearch ${ params.query.url }`);
            $.ajax({
                url: params.query.url,
                dataType: 'jsonp',
                jsonp: 'json.wrf',
                failure: function(err) {
                    console.error({code:'widget.latch.query.failed',args:[params.query.displayTitle,err.responseText]});
                },
                success: function(data){
                    loader.hide();

                    tracker.trackEvent(params.query.creator + "",data.response.numFound);

                    if(data.response.numFound == 0) {
                        resultCount.text(Utils.localize({code:'widget.latch.query.no-results'}));
                        return;
                    }

                    //map to search results
                    var docs = $.map(data.response.docs, function(doc) {
                        return {
                            source: params.query.resultConfig.mappings.source ? doc[params.query.resultConfig.mappings.source] : undefined,
                            title: params.query.resultConfig.mappings.title ? doc[params.query.resultConfig.mappings.title] : undefined,
                            description: params.query.resultConfig.mappings.description ? doc[params.query.resultConfig.mappings.description] : undefined,
                            type: params.query.resultConfig.mappings.type ? doc[params.query.resultConfig.mappings.type] : undefined,
                            doctype: params.query.resultConfig.mappings.doctype ? (Utils.mapDocType(doc[params.query.resultConfig.mappings.doctype])) : undefined,
                            link: params.query.resultConfig.mappings.link ? doc[params.query.resultConfig.mappings.link] : undefined,
                            date: params.query.resultConfig.mappings.date ? new Date(doc[params.query.resultConfig.mappings.date]) : undefined
                        };
                    });

                    resultCount.text(Utils.localize({code:'widget.latch.query.header',args:[data.response.numFound]}));

                    $.each(docs,function(i,doc){
                        var docli = $('<li>' +
                            (doc.thumb ? '<div class="result-type"><div class="result-avatar-image" style="background-image:url(\''+doc.thumb+'\')"></div></div>' : '<div class="result-type result-type-'+doc.doctype+'"><div>'+doc.doctype+'</div></div>') +
                            '<div class="result-content"><div class="result-content-title"><a href="'+doc.link+'" target="blank">'+doc.title+'</a><span>'+(doc.date ? doc.date.toLocaleDateString() : '')+'</span></div>' + (doc.description ? '<p>'+doc.description+'</p>' : '') + '</div>' +
                            '<div class="result-actions"><button class="postAnswer">Posten<i class="icon-paper-plane"></i></button></div>'+
                            (i+1 != docs.length ? '<li class="result-separator"><div></div></li>':'') +
                            '</li>');

                        docli.find('.postAnswer').click(function(){
                            var text = Utils.localize({code:"widget.latch.answer.title",args:[params.query.displayTitle]});
                            var attachments = [{
                                title: doc.title,
                                title_link: doc.link,
                                thumb_url: doc.thumb ? doc.thumb : undefined,
                                text:doc.description
                            }];
                            if(options.postings && options.postings.type == 'suggestText') {
                                messageInputField.post(text + '\n' + '[' + doc.title + '](' + doc.link + '): ' + doc.description);
                            } else if(options.postings && options.postings.type == 'postText') {
                                smarti.post(text + '\n' + '[' + doc.title + '](' + doc.link + '): ' + doc.description,[]);
                            } else {
                                smarti.post(text,attachments);
                            }

                            tracker.trackEvent(params.query.creator + ".result.post", (page*numOfRows) + i);
                        });

                        results.append(docli);
                    });

                    var prev = $('<span>').text(Utils.localize({code:'widget.latch.query.paging.prev'})).prepend('<i class="icon-angle-left">');
                    var next = $('<span>').text(Utils.localize({code:'widget.latch.query.paging.next'})).append('<i class="icon-angle-right">');;

                    if(page > 0) {
                        prev.click(function(){
                            tracker.trackEvent(params.query.creator + ".result.paging", page-1);
                            getResults(page-1)
                        });
                    } else {
                        prev.hide();
                    }

                    if((data.response.numFound/numOfRows) > (page+1)) {
                        next.addClass('active').click(function(){
                            tracker.trackEvent(params.query.creator + ".result.paging", page+1);
                            getResults(page+1)
                        });
                    } else {
                        next.hide();
                    }

                    $('<tr>')
                        .append($('<td class="pageLink pageLinkLeft">').append(prev))
                        .append($('<td class="pageNum">').text((page+1)+'/'+Math.ceil(data.response.numFound/numOfRows)))
                        .append($('<td class="pageLink pageLinkRight">').append(next))
                        .appendTo(resultPaging);

                }
            });
        }

        getResults(0);

        return {
            refresh: refresh
        }
    }

    /**
     * @param params
     * @param wgt_conf
     * @returns {
     *      refresh: FUNCTION
     * }
     * @constructor
     */
    function ConversationWidget(params,wgt_config) {

        params.elem.append('<h2>' + Utils.localize({code:'widget.conversation.title'}) + '</h2>');

        function refresh(data) {
            getResults();
        }

        var loader = $('<div class="loading-animation"> <div class="bounce1"></div> <div class="bounce2"></div> <div class="bounce3"></div> </div>').hide().appendTo(params.elem);
        var msg = $('<div class="no-result">').appendTo(params.elem);
        var results = $('<ul class="search-results">').appendTo(params.elem);

        function getResults() {

            //TODO get remote
            results.empty();
            loader.show();

            smarti.query({conversationId:params.id,template:params.tempid,creator:params.query.creator},function(data){

                loader.hide();

                if(data.length == 0) {
                    msg.text(Utils.localize({code:'widget.conversation.no-results'}));
                } else {
                    msg.empty();
                }

                function buildLink(msgid) {
                    return "http://permalink.org?msg="+msgid; //TODO permalinks
                }

                function buildAttachments(doc) {
                    var attachment = {
                        author_name: "\t",
                        author_icon: Utils.getAvatarUrl(doc.userName),
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

                    function getSubcontent(docs,mainUser) {
                        var result = $('<ul>');

                        $.each(docs, function(j,subdoc){

                            var liClass = mainUser == subdoc.userName ? 'question' : 'answer';

                            result.append($('<li>')
                                .addClass(liClass)
                                .append('<div class="subdoc-title"><img src="'+Utils.getAvatarUrl(subdoc.userName)+'">' +
                                        '<span>'+(new Date(subdoc.timestamp)).toLocaleDateString()+'</span></div>')
                                    .append('<div class="subdoc-content">'+subdoc.content.replace(/\n/g, "<br />")+'</div>')
                                    .append($('<div>').addClass('result-actions').append(
                                        $('<button>').addClass('postMessage').click(function(){

                                            var text = Utils.localize({code:"widget.conversation.answer.title_msg"});
                                            var attachments = [buildAttachments(subdoc)];

                                            if(options.postings && options.postings.type == 'suggestText') {
                                                messageInputField.post(text + '\n' + '*' + Utils.getAnonymUser(subdoc.userName) + '*: ' + subdoc.content.replace(/\n/g, " "));
                                            } else if(options.postings && options.postings.type == 'postText') {
                                                smarti.post(text + '\n' + '*' + Utils.getAnonymUser(subdoc.userName) + '*: ' + subdoc.content.replace(/\n/g, " "),[]);
                                            } else {
                                                smarti.post(text,attachments);
                                            }

                                            tracker.trackEvent("conversation.part.post", i);
                                        }).append('<i class="icon-paper-plane"></i>')
                                    ))
                            );
                        });

                        return result;
                    }

                    var docli = $('<li>')
                        .append('<div class="result-type"><div class="result-avatar-image" style="background-image:url(\''+Utils.getAvatarUrl(doc.userName)+'\')"></div></div>')
                        .append($('<div>').addClass('result-content')
                            .append($('<div>').addClass('result-content-title')
                                .append('<span class="date-only">' + (new Date(doc.timestamp)).toLocaleString() + '</span>')
                                .append($('<span>').addClass('toggle').addClass('icon-right-dir').click(function(e){
                                        $(e.target).parent().parent().parent().find('.result-subcontent').toggle();
                                        if($(e.target).hasClass('icon-right-dir')) {
                                            tracker.trackEvent("conversation.part.open", i);
                                            $(e.target).removeClass('icon-right-dir').addClass('icon-down-dir');
                                        } else {
                                            tracker.trackEvent("conversation.part.close", i);
                                            $(e.target).removeClass('icon-down-dir').addClass('icon-right-dir');
                                        }
                                    })
                                ))
                            .append('<p>' + doc.content.replace(/\n/g, "<br />") + '</p>'))
                        .append($('<div class="result-subcontent">')
                            .append(getSubcontent(doc.answers,doc.userName)).hide())
                        .append($('<div>').addClass('result-actions').append(
                            $('<button>').addClass('postAnswer').addClass('button').text(Utils.localize({code:'widget.conversation.post-all',args:[doc.answers.length+1]})).click(function(){
                                var text = Utils.localize({code:'widget.conversation.answer.title'});
                                var attachments = [buildAttachments(doc)];

                                function createTextMessage() {
                                    text = text + '\n' + '*' + Utils.getAnonymUser(doc.userName) + '*: ' + doc.content.replace(/\n/g, " ");
                                    $.each(doc.answers, function(i,answer) {
                                        text += '\n*' + Utils.getAnonymUser(answer.userName) + '*: ' + answer.content.replace(/\n/g, " ");
                                    });
                                    return text;
                                }

                                if(options.postings && options.postings.type == 'suggestText') {
                                    messageInputField.post(createTextMessage());
                                } else if(options.postings && options.postings.type == 'postText') {
                                    smarti.post(createTextMessage(),[]);
                                } else {
                                    smarti.post(text,attachments);
                                }

                                tracker.trackEvent("conversation.post", i);
                            }).append('<i class="icon-paper-plane"></i>')));

                        if(i + 1 != data.length) {
                            docli.append('<li class="result-separator"><div></div></li>');
                        }

                    results.append(docli);
                })

            });
        }

        getResults();

        return {
            refresh: refresh
        }
    }

    //Main layout
    element = $(element);

    element.empty();

    var mainDiv = $('<div>').addClass('widget').appendTo(element);

    var contentDiv = $('<div>').appendTo(mainDiv);
    var messagesDiv = $('<div>').appendTo(mainDiv);

    //Smarti

    var smarti = new Smarti({DDP:{endpoint:options.socketEndpoint},tracker:tracker,smarti:{endpoint:options.smartiEndpoint},channel:options.channel,rocket:{endpoint:options.rocketBaseurl}});//TODO wait for connect?

    smarti.subscribe('smarti.data', function(data){
        refreshWidgets(data);
    });

    function showError(err) {
        messagesDiv.empty().append($('<p>').text(Utils.localize(err)));
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

                    var constructor = undefined;

                    switch(template.type) {
                        case 'ir_latch':
                            constructor = IrLatchWidget;break;
                        case 'related.conversation':
                            constructor = ConversationWidget;break;
                    }

                    if(constructor && (options.widget[query.creator] ? !options.widget[query.creator].disabled : true)) {
                        var elem = $('<div class="smarti-widget">').appendTo(contentDiv);

                        var params = {
                            elem:elem,
                            id:data.id,
                            slots:template.slots,
                            tempid:i,
                            tokens:data.tokens,
                            query:query
                        };

                        var config = options.widget[query.creator] || {};

                        widgets.push(new constructor(params,config))
                    }

                })
            });

            if(!widgets.length > 0) {
                showError({code:'smarti.result.no-result-yet'});
            } else {
                initialized = true;
            }
        } else {
            $.each(widgets, function(i,wgt){
                wgt.refresh(data);
            })
        }
    }

    function initialize() {
        smarti.init(null,showError);
    }

    smarti.login(
        initialize,
        drawLogin
    );

    //append lightbulb close (only one handler!)
    var tabOpenButton = $('.flex-tab-container .flex-tab-bar .icon-lightbulb').parent();

    tabOpenButton.unbind('click.closeTracker');

    tabOpenButton.bind('click.closeTracker', function() {
        if($('.external-search-content').is(":visible")) {
            tracker.trackEvent('sidebar.close');
        }
    });

    return {}; //whatever is necessary..
}

window.SmartiWidget = SmartiWidget;
