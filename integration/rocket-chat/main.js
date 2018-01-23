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
const md5 = require('js-md5');
const moment = require('moment');
//$ = require('jquery');
require('jsviews');

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
    "smarti.channels": {
        "de": "Kanäle",
        "en": "Channels"
    },
    "smarti.new-search-term": {
        "de": "Neuer Suchterm",
        "en": "New search term"
    },
    "smarti.date-format": {
        "de": "DD.MM.YYYY HH:mm:ss",
        "en": "DD-MM-YYYY HH:mm:ss"
    },
    "msg.post.failure": {
        "de": "Nachricht konnte nicht gepostet werden",
        "en": "Posting message failed"
    },
    "get.conversation.params": {
        "de": "Konversations-Parameter konnten nicht geladen werden: $[1]",
        "en": "Cannot load conversation params: $[1]"
    },
    "widget.post": {
        "de": "Posten",
        "en": "Post"
    },
    "widget.post-message": {
        "de": "Nachricht posten",
        "en": "Post message"
    },
    "widget.post-conversation": {
        "de": "Konversation posten",
        "en": "Post conversation"
    },
    "widget.answers": {
        "de": "Antworten",
        "en": "answers"
    },
    "widget.tags.label": {
        "de": "Ergebnisse zu:",
        "en": "Results for:"
    },
    "widget.latch.query.failed":{
        "de": "Widget $[1] hat Probleme bei der Anfrage: $[2]",
        "en": "Widget $[1] has problems while quering: $[2]"
    },
    "widget.latch.query.no-results":{
        "de":"Keine Ergebnisse",
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
    "widget.latch.query.remove.all": {
        "en": "Clear all",
        "de": "Alle löschen"
    },
    "widget.latch.query.remove": {
        "en": "remove",
        "de": "löschen"
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
    },
    "widget.conversation.post-selected": {
        "de": "Selektierte Nachricht posten",
        "en": "Post selected message"
    },
    "widget.conversation.post-selected-all": {
        "de": "$[1] selektierte Nachrichten posten",
        "en": "Post $[1] selected messages"
    }
}, undefined, 'xx');

const Utils = {
    getAvatarUrl : function(id) {
        return "https://www.gravatar.com/avatar/"+md5('redlink'+id)+"?d=identicon";
    },
    getAnonymUser: function(id) {
        return 'User-' + (parseInt(md5('redlink'+id),16)%10000); //TODO check if this works somehow...
    },
    localize: function(obj) {
        if(obj.args && obj.args.length > 0) {
            const args_clone = ld_lang.cloneDeep(obj.args);
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
    },
    cropLabel: function(label,max_length,replace,mode) {
        max_length = max_length-replace.length;

        if(label.length <= max_length) return label;

        switch(mode) {
            case 'start': return label.substring(0,max_length) + replace;
            case 'end': return replace + label.substring(label.length-max_length);
            default:
                const partLength = Math.floor(max_length/2);
                return label.substring(0,partLength) + replace + label.substring(label.length-partLength);
        }
    }
};

/**
 * Manages the interaction between Plugin and RocketChat/Smarti
 * @param {Object} options: {
 *      DDP: {
 *          endpoint: STRING
 *      },
 *      channel: STRING,
 *      rocket: {
 *          socketEndpoint: STRING
 *      },
 *      lang: STRING
 *      tracker: Tracker
 * }
 * @returns {Object} {
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
    let ddp  = new DDP(options.DDP);

    let pubsubs = {};
    /**
     * Enabled publication-subscription mechanism
     * @param id
     * @returns {*}
     */
    //taken from http://api.jquery.com/jQuery.Callbacks/
    function pubsub( id ) {
        let callbacks, pubsub = id && pubsubs[ id ];

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
     *

     */
    function login(success,failure,username,password) {

        /**
         * Login to meteor ddp sockets with params
         * @param params
         */
        function loginRequest(params) {
            const loginId = ddp.method("login", params);
            ddp.on('result', function (message) {

                if (message.id === loginId) {
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

        } else if(
            localStorage &&
            localStorage.getItem('Meteor.loginToken') &&
            localStorage.getItem('Meteor.loginTokenExpires') &&
            (new Date(localStorage.getItem('Meteor.loginTokenExpires')) > new Date())
        ) {
            console.debug(
                'found token %s for user %s that expires on %s',
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
     * Initializes the Smarti widget, by
     *
     * - get the conversation Id for the clinet's channel
     * - subscribing the given/entered channel for changes
     *
     * @param failure - a function called on any errors to display a message
     */
    function init(failure) {

        // fetch last Smarti results when the wiget gets initialized (channel entered)
        console.debug('Smarti widget init -> try get last Smarti result for channel', options.channel);
        const lastConvCallId = ddp.method("getConversationId", [options.channel]);
        ddp.on("result", function(message) {
            if (message.error) {
                return failure({code:"get.conversation.params", args:[message.error.reason]});
            } else if(message.id === lastConvCallId) {
                if(message.result) {
                    // message found for channel -> fetch conversation results
                    console.debug('Smarti widget init -> get last conversation result for message:', message.result);
                    getConversation(message.result, failure);
                } else {
                    return failure({code:'smarti.result.no-result-yet'});
                }
            }
        });

        // subscribe changes on the channel that is passed by SmartiWidget constructor (message send)
        console.debug('Smarti widget init -> subscribe channel', options.channel);
        const subId = ddp.sub("stream-notify-room", [options.channel+"/newConversationResult", false]);
        ddp.on("nosub", function() {
            failure({code:'sub.new-conversation-result.nosub'});
        });
        ddp.on("changed", function(message) {
            if(message.id == subId) {
                // subscriotion has changed (message send) -> fetch conversation results
                console.debug('Smarti widget subscription changed -> get conversation result for message:', message.fields.args[0]);
                pubsub('smarti.data').publish(message.fields.args[0]);
            }
        });
    }

    /**
     * Fetches the analyzed conversation by Id
     *
     * @param conversationId - the conversation Id to get the Smarti results for
     * @param failure - a function called on any errors to display a message
     */
    function getConversation(conversationId, failure) {

        console.debug('fetch results for conversation with Id:', conversationId);
        const msgid = ddp.method("getConversation",[conversationId]);
        ddp.on("result", function(message) {

            if (message.error) {
                return failure({code:"get.conversation.params", args:[message.error.reason]});
            } else if(message.id === msgid) {
                if(message.result) {
                    // why don't call query() from here instead using a subscrition?
                    pubsub('smarti.data').publish(message.result);
                } else {
                    if(failure) failure({code:'smarti.result.no-result-yet'});
                }
            }
        });
    }

    function query(params, success, failure) {

      console.debug('get query builder result for conversation with [id: %s, templateIndex: %s, creatorName: %s, start: %s}', params.conversationId, params.template, params.creator, params.start);
      const msgid = ddp.method("getQueryBuilderResult",[params.conversationId, params.template, params.creator, params.start]);
      ddp.on("result", function(message) {

          if (message.error) return failure({code:"get.query.params",args:[message.error.reason]});

          if(message.id === msgid) {
              if(message.result) {
                  success(message.result);
              } else {
                  if(failure) failure({code:'smarti.result.no-result-yet'});
              }
          }
      });
    }

    /**
     * Posts a Smarti result to the message input field of Rocket.Chat
     *
     * @param msg
     * @param attachments
     * @param success
     * @param failure
     */
    function post(msg,attachments,success,failure) {
        const methodId = ddp.method("sendMessage",[{rid:options.channel,msg:msg,attachments:attachments,origin:'smartiWidget'}]);

        ddp.on("result", function(message) {
            if(message.id === methodId) {
                if(message.error && failure) {

                    console.debug('cannot post message:\n', JSON.stringify(message.error,null,2));

                    if(failure) failure({code:"msg.post.failure"});
                }
                else if(success) success();
            }
        });
    }

    return {
        login: login,
        init: init,
        subscribe: (id, func) => pubsub(id).subscribe(func),
        unsubscribe: (id, func) => pubsub(id).unsubscribe(func),
        query: query,
        post: post
    };
}

/**
 * A tracker wrapper
 * @param category
 * @param roomId
 * @param onEvent the real tracker methode which is called
 * @constructor
 */
function Tracker(category, roomId, onEvent) {
    this.trackEvent = function(action, value) {
        console.debug(`track event: ${category}, ${action}, ${roomId}, ${value}`);
        if(onEvent) onEvent(category, action, roomId, value);
    };

}

/**
 * Generates a smarti widget and appends it to element
 *
 * @param {element} element a dom element
 *
 * @param {Object} _options: {
 *       socketEndpoint: 'ws://localhost:3000/websocket/',
 *       channel: 'GENERAL',
 *       postings: {
 *          type: 'WIDGET_POSTING_TYPE',
 *          cssInputSelector: '.rc-message-box .js-input-message'
 *       }
 *       lang:'de'
 *   }
 *
 * @returns {Object}
 *
 * @constructor
 */
function SmartiWidget(element, _options) {

    let options;
    let initialized = false;

    options = {
        socketEndpoint: 'ws://localhost:3000/websocket/',
        channel: 'GENERAL',
        postings: {
            type: 'suggestText', // possible values: suggestText, postText, postRichText
            cssInputSelector: '.message-form-text.input-message'
        },
        widget: {},
        tracker: {
            onEvent: (typeof Piwik !== 'undefined' && Piwik) ? Piwik.getTracker().trackEvent : function() {
            },
            category: "knowledgebase"
        },
        ui: {
            tokenpill: {
                textlength: 30,
                textreplace: '...',
                textreplacemode: 'middle'
            }
        },
        lang: 'de'
    };

    $.extend(true,options,_options);

    console.debug('init smarti widget:\n', JSON.stringify(options,null,2));

    localize.setLocale(options.lang);

    let tracker = new Tracker(options.tracker.category,options.channel,options.tracker.onEvent);

    let widgets = [];

    let messageInputField;

    function InputField(elem) {
        this.post = function(msg) {
            console.debug(`write text to element: ${msg}`);
            elem.val(msg);
            elem.focus();
        };
    }

    if(options.postings && options.postings.type === 'suggestText') {
        if(options.postings.cssInputSelector) {
            let inputFieldELement = $(options.postings.cssInputSelector);
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
     * @param {Object} params
     * @param {Object} params.query
     * @param {Object} params.query.resultConfig
     * @param wgt_conf
     * @returns {Object} {
     *      refresh: FUNCTION
     * }
     * @constructor
     */
    function IrLatchWidget(params,wgt_conf) {
        widgetIrLatchTemplate.link(params.elem, params.templateData);

        const numOfRows = wgt_conf.numOfRows || params.query.resultConfig.numOfRows;

        function refresh() {
            getResults(0);
        }

        function getResults(page, useSearchTerms) {

            let tks = widgetHeaderTagsTemplateData.tokens.map(t => t.value).concat(widgetHeaderTagsTemplateData.userTokens);
            if(useSearchTerms) tks = tks.concat(searchTerms || []);
            tks = tks.join(" ");

            let queryParams = {
                'wt': 'json',
                'fl': '*,score',
                'rows': numOfRows,
                'q':  tks
            };

            params.query.url = params.query.url.substring(0, params.query.url.indexOf('?')) + '?';

            //append params
            for(let property in params.query.defaults) {
                if (params.query.defaults.hasOwnProperty(property))
                    queryParams[property] = params.query.defaults[property];
            }
            queryParams.start = page > 0 ? (page*numOfRows) : 0;

            // external Solr search
            console.log(`executeSearch ${ params.query.url }, with`, queryParams);
            $.observable(params.templateData).setProperty("loading", true);
            $.ajax({
                url: params.query.url,
                data: queryParams,
                traditional: true,
                dataType: 'jsonp',
                jsonp: 'json.wrf',
                failure: function(err) {
                    console.error(Utils.localize({code:'widget.latch.query.failed', args:[params.query.displayTitle, err.responseText]}));
                },
                /**
                 *
                 * @param {Object} data
                 * @param {Object} data.response
                 * @param {Number} data.response.numFound
                 *
                 */
                success: function(data) {
                    tracker.trackEvent(params.query.creator + "",data.response.numFound);

                    console.log(params.query);
                    console.log(data.response.docs);

                    //map to search results
                    let docs = $.map(data.response.docs, function(doc) {
                        let newDoc = {};
                        Object.keys(params.query.resultConfig.mappings).forEach(k => {
                            let v = params.query.resultConfig.mappings[k];
                            if(v) {
                                if(k === "doctype") {
                                    newDoc[k] = Utils.mapDocType(doc[v]);
                                } else if(k === "date") {
                                    newDoc[k] = new Date(doc[v]).getTime();
                                } else if(k === "link") {
                                    newDoc[k] = Array.isArray(doc[v]) ? doc[v][0] : doc[v];
                                } else if(k === "type") {
                                    newDoc[k] = doc[v].split(".").pop();
                                } else {
                                    newDoc[k] = doc[v];
                                }
                            }
                        });
                        return newDoc;
                    }).filter(doc => {
                        // required fields
                        return doc.title;
                    });

                    if(docs & docs.length) {
                        docs.forEach(d => {
                            d.templateType = "ir_latch";
                        });
                    }

                    console.log(docs);

                    $.observable(params.templateData).setProperty("loading", false);
                    $.observable(params.templateData.results).refresh(docs);
    
                    //resultCount.text(Utils.localize({code:'widget.latch.query.header',args:[data.response.numFound]}));
                    
                    /*
                    $.each(docs,function(i,doc) {
                        let docli = $('<li>' +
                            (doc.thumb ? '<div class="result-type"><div class="result-avatar-image" style="background-image:url(\''+doc.thumb+'\')"></div></div>' : '<div class="result-type result-type-'+doc.doctype+'"><div>'+doc.doctype+'</div></div>') +
                            '<div class="result-content"><div class="result-content-title"><a href="'+doc.link+'" target="blank">'+doc.title+'</a><span>'+(doc.date ? doc.date.toLocaleDateString() : '')+'</span></div>' + (doc.description ? '<p>'+doc.description+'</p>' : '') + '</div>' +
                            '<div class="result-actions"><button class="postAnswer">Posten<i class="icon-paper-plane"></i></button></div>'+
                            (i+1 !== docs.length ? '<li class="result-separator"><div></div></li>':'') +
                            '</li>');

                        docli.find('.postAnswer').click(function() {
                            let text = Utils.localize({code:"widget.latch.answer.title",args:[params.query.displayTitle]});
                            let attachments = [{
                                title: doc.title,
                                title_link: doc.link,
                                thumb_url: doc.thumb ? doc.thumb : undefined,
                                text:doc.description
                            }];
                            if(options.postings && options.postings.type === 'suggestText') {
                                messageInputField.post(text + '\n' + '[' + doc.title + '](' + doc.link + '): ' + doc.description);
                            } else if(options.postings && options.postings.type === 'postText') {
                                smarti.post(text + '\n' + '[' + doc.title + '](' + doc.link + '): ' + doc.description,[]);
                            } else {
                                smarti.post(text,attachments);
                            }

                            tracker.trackEvent(params.query.creator + ".result.post", (page*numOfRows) + i);
                        });

                        results.append(docli);
                    });

                    
                    let prev = $('<span>').text(Utils.localize({code:'widget.latch.query.paging.prev'})).prepend('<i class="icon-angle-left">');
                    let next = $('<span>').text(Utils.localize({code:'widget.latch.query.paging.next'})).append('<i class="icon-angle-right">');

                    if(page > 0) {
                        prev.click(function() {
                            tracker.trackEvent(params.query.creator + ".result.paging", page-1);
                            getResults(page-1)
                        });
                    } else {
                        prev.hide();
                    }

                    if((data.response.numFound/numOfRows) > (page+1)) {
                        next.addClass('active').click(function() {
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
                    */
                }
            });
        }

        getResults(0);

        return {
            params,
            refresh,
            getResults,
            templateType: "ir_latch"
        };
    }

    /**
     * @param params
     * @returns {Object} {
     *      refresh: FUNCTION
     * }
     * @constructor
     */
    function ConversationWidget(params) {
        widgetConversationTemplate.link(params.elem, params.templateData);
    
        function refresh() {
            getResults(0);
        }

        //let resultPaging = $('<table>').addClass('paging').appendTo(params.elem);

        function getResults(page, pageSize) {

            //resultPaging.empty();

            $.observable(params.templateData).setProperty("loading", true);
            
            let start = pageSize ? page * pageSize : 0;

            smarti.query({
                conversationId: params.id,
                template: params.tempid,
                creator: params.query.creator,
                start: start
            }, function(data) {

                if(data.docs & data.docs.length) {
                    data.docs.forEach(d => {
                        d.templateType = "related.conversation";
                    });
                }

                console.log(data);

                $.observable(params.templateData).setProperty("loading", false);
                $.observable(params.templateData.results).refresh(data.docs);
        
                /*
                $.each(data.docs, function (i, doc) {

                    function getSubcontent(docs,mainUser) {
                        let result = $('<ul>');

                        $.each(docs, function(j,subdoc) {

                            let liClass = mainUser === subdoc.userName ? 'question' : 'answer';

                            result.append($('<li>')
                                .addClass(liClass)
                                .append('<div class="subdoc-title"><img src="'+Utils.getAvatarUrl(subdoc.userName)+'">' +
                                        '<span>'+(new Date(subdoc.timestamp)).toLocaleDateString()+'</span></div>')
                                    .append('<div class="subdoc-content">'+subdoc.content.replace(/\n/g, "<br />")+'</div>')
                                    .append($('<div>').addClass('result-actions').append(
                                        $('<button>').addClass('postMessage').click(function() {

                                            let text = Utils.localize({code:"widget.conversation.answer.title_msg"});
                                            let attachments = [buildAttachments(subdoc)];

                                            if(options.postings && options.postings.type === 'suggestText') {
                                                messageInputField.post(text + '\n' + '*' + Utils.getAnonymUser(subdoc.userName) + '*: ' + subdoc.content.replace(/\n/g, " "));
                                            } else if(options.postings && options.postings.type === 'postText') {
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

                    let docli = $('<li>')
                        .append('<div class="result-type"><div class="result-avatar-image" style="background-image:url(\''+Utils.getAvatarUrl(doc.userName)+'\')"></div></div>')
                        .append($('<div>').addClass('result-content')
                            .append($('<div>').addClass('result-content-title')
                                .append('<span class="date-only">' + (new Date(doc.timestamp)).toLocaleString() + '</span>')
                                .append($('<span>').addClass('toggle').addClass('icon-right-dir').click(function(e) {
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
                            $('<button>').addClass('postAnswer').addClass('button').text(Utils.localize({code:'widget.conversation.post-all',args:[doc.answers.length+1]})).click(function() {
                                let text = Utils.localize({code:'widget.conversation.answer.title'});
                                let attachments = [buildAttachments(doc)];

                                function createTextMessage() {
                                    text = text + '\n' + '*' + Utils.getAnonymUser(doc.userName) + '*: ' + doc.content.replace(/\n/g, " ");
                                    $.each(doc.answers, function(i,answer) {
                                        text += '\n*' + Utils.getAnonymUser(answer.userName) + '*: ' + answer.content.replace(/\n/g, " ");
                                    });
                                    return text;
                                }

                                if(options.postings && options.postings.type === 'suggestText') {
                                    messageInputField.post(createTextMessage());
                                } else if(options.postings && options.postings.type === 'postText') {
                                    smarti.post(createTextMessage(),[]);
                                } else {
                                    smarti.post(text,attachments);
                                }

                                tracker.trackEvent("conversation.post", i);
                            }).append('<i class="icon-paper-plane"></i>')));

                        if(i + 1 !== data.length) {
                            docli.append('<li class="result-separator"><div></div></li>');
                        }

                    results.append(docli);
                });
                

                let prev = $('<span>').text(Utils.localize({code:'widget.latch.query.paging.prev'})).prepend('<i class="icon-angle-left">');
                let next = $('<span>').text(Utils.localize({code:'widget.latch.query.paging.next'})).append('<i class="icon-angle-right">');

                if(page > 0) {
                    prev.click(function() {
                        tracker.trackEvent(params.query.creator + ".result.paging", page-1);
                        getResults(page-1,data.pageSize)
                    });
                } else {
                    prev.hide();
                }

                if((data.numFound/data.pageSize) > (page+1)) {
                    next.addClass('active').click(function() {
                        tracker.trackEvent(params.query.creator + ".result.paging", page+1);
                        getResults(page+1,data.pageSize)
                    });
                } else {
                    next.hide();
                }

                $('<tr>')
                    .append($('<td class="pageLink pageLinkLeft">').append(prev))
                    .append($('<td class="pageNum">').text((page+1)+'/'+Math.ceil(data.numFound/data.pageSize)))
                    .append($('<td class="pageLink pageLinkRight">').append(next))
                    .appendTo(resultPaging);
                */
            }, showError
          );
        }

        getResults(0);

        return {
            params,
            refresh,
            getResults,
            templateType: "related.conversation"
        };
    }

    function showError(err) {
        widgetMessage.empty().append($('<p>').text(Utils.localize(err)));
    }

    function drawLogin() {

        widgetContent.empty();

        let form = $('<form><span>Username</span><input type="text"><br><span>Password</span><input type="password"><br><button>Submit</button></form>');

        form.find('button').click(function() {

            let username = form.find('input[type="text"]').val();
            let password = form.find('input[type="password"]').val();

            smarti.login(
                initialize,
                showError,
                username,
                password
            );

            return false;
        });

        form.appendTo(widgetContent);

    }

    function refreshWidgets(data) {
        let tokens = data.tokens.filter(t => t.type != "Attribute").sort((a, b) => {
            return a.messageIdx - b.messageIdx;
        }).reverse();
        let filteredTokens = {};
        let uniqueTokens = tokens.filter(function(t) {
            if (filteredTokens[t.value]) return false;
            filteredTokens[t.value] = true;
            return true;
        }).reverse().slice(-7);
        console.log("Filtered tokens:", uniqueTokens);
        $.observable(widgetHeaderTagsTemplateData.tokens).refresh(uniqueTokens);

        if(!initialized) {
            widgetContent.empty();
            widgetMessage.empty();

            $.each(data.templates, function(i, template) {
                $.each(template.queries, function(j, query) {

                    let constructor;

                    switch(template.type) {
                        case 'ir_latch':
                            constructor = IrLatchWidget;break;
                        case 'related.conversation':
                            constructor = ConversationWidget;break;
                    }

                    
                    if(constructor && (!options.widget[query.creator] || !options.widget[query.creator].disabled)) {
                        let elem = $('<div class="smarti-widget">').hide().appendTo(widgetContent);

                        let params = {
                            elem: elem,
                            templateData: {loading: true, results: []},
                            id: data.conversation,
                            slots: template.slots,
                            type: template.type,
                            tempid: i,
                            tokens: data.tokens,
                            query: query
                        };

                        let config = options.widget[query.creator] || {};

                        $.observable(widgets).insert(new constructor(params, config));
                    }

                });
            });

            if(widgets.length > 0) {
                initNavTabs();
                initialized = true;
            } else {
                showError({code:'smarti.result.no-result-yet'});
            }
        } else {
            $.each(widgets, function(i,wgt) {
                wgt.refresh();
            });
        }
    }

    function initialize() {
        smarti.init(showError);
    }

    function initNavTabs() {
        tabs.show();
        tabs.find('.moreSources li').first().click();
    }

    const widgetHeaderTagsTemplateStr = `
        <span>${Utils.localize({code: 'widget.tags.label'})}</span>
        <ul>
            <li class="add">+</li>
            {^{for userTokens}}
            <li>{{:}}</li>
            {{/for}}
            {^{for tokens}}
            <li>{{:value}}</li>
            {{/for}}
            <li class="remove">${Utils.localize({code: 'widget.latch.query.remove.all'})}</li>
        </ul>
    `;
    const widgetHeaderTabsTemplateStr = `
        <div id="tabContainer">
            <span class="nav-item current">{^{if widgets.length}}{^{:widgets[selectedWidget].params.query.displayTitle}} ({^{:widgets[selectedWidget].params.templateData.results.length || 0}}){{/if}}</span>
            <span class="nav-item more">${Utils.localize({code: 'smarti.channels'})}</span>
        </div>
        <ul class="moreSources">
            {^{for widgets}}
            <li>{^{:params.query.displayTitle}} ({^{:params.templateData.results.length || 0}})</li>
            {{/for}}
        </ul>
    `;
    const widgetHeaderInnerTabSearchTemplateStr = `
        <input type="search" placeholder="" data-link="placeholder{: 'Suchen in &#34;' + containerTitle + '&#34;' }">
        <div id="innerTabSearchSubmit">
            <div class="submit-icon"></div>
        </div>
    `;
    const widgetFooterPostButtonTemplateStr = `
        <span><i class="icon-paper-plane"></i> {^{:title}}</span>
    `;
    const widgetConversationTemplateStr = `
        {^{if loading}}
            <div class="loading-animation"><div class="bounce1"></div><div class="bounce2"></div><div class="bounce3"></div></div>
        {{else}}
            {^{for results}}
                <div class="conversation">
                    <div class="convMessage" data-link="class{merge: answers toggle='parent'}">
                        <div class="middle">
                            <div class="datetime">
                                {{tls:timestamp}}
                                {^{if isTopRated}}<span class="topRated">Top</span>{{/if}}
                                <span class="answers">{{: answers.length}} ${Utils.localize({code: 'widget.answers'})}</span>
                            </div>
                            <div class="title"></div>
                            <div class="text"><p>{{nl:~hl(content, true)}}</p></div>
                            <div class="postAction">{^{if answers.length}}${Utils.localize({code: 'widget.post-conversation'})}{{else}}${Utils.localize({code: 'widget.post-message'})}{{/if}}</div>
                            <div class="selectMessage"></div>
                        </div>
                    </div>
                    {^{if answers.length}}
                        <div class="responseContainer">
                            {^{for answers}}
                                <div class="convMessage">
                                    <div class="middle">
                                        <div class="datetime">
                                        {{tls:timestamp}}
                                        </div>
                                        <div class="title"></div>
                                        <div class="text"><p>{{nl:~hl(content, true)}}</p></div>
                                        <div class="postAction">${Utils.localize({code: 'widget.post-message'})}</div>
                                        <div class="selectMessage"></div>
                                    </div>
                                </div>
                            {{/for}}
                        </div>
                    {{/if}}
                </div>
            {{else}}
                <div class="no-result">${Utils.localize({code: 'widget.conversation.no-results'})}</div>
            {{/for}}
        {{/if}}
    `;
    const widgetIrLatchTemplateStr = `
        {^{if loading}}
            <div class="loading-animation"><div class="bounce1"></div><div class="bounce2"></div><div class="bounce3"></div></div>
        {{else}}
            {^{for results}}
                <div class="irl-result">
                    <div class="middle">
                        <div class="datetime">
                            {^{tls:date}}
                        </div>
                        {^{if link}}
                            <div class="title"><a data-link="href{:link}" target="_blank">{^{:~hl(title)}}</a></div>
                        {{else}}
                            <div class="title">{^{:~hl(title)}}</div>
                        {{/if}}
                        <div class="text"><p>{^{:~hl(description)}}</p></div>
                        {^{if source}}<span class="source">{^{:source}}</span>{{/if}}
                        {^{if type}}<span class="type">{^{:type}}</span>{{/if}} <br />
                        <div class="postAction">${Utils.localize({code: 'widget.post'})}</div>
                        <div class="selectMessage"></div>
                    </div>
                </div>
            {{else}}
                <div class="no-result">${Utils.localize({code: 'widget.latch.query.no-results'})}</div>
            {{/for}}
        {{/if}}
    `;
    
    //Main layout
    element = $(element);

    let widgetHeader = element.find('#widgetHeader');
    let widgetHeaderWrapper = widgetHeader.find('.widgetHeaderWrapper');
    let widgetTitle = widgetHeaderWrapper.find('h4');
    let widgetBody = element.find('#widgetBody');
    let widgetMessage = widgetBody.find('.widgetMessage');
    let widgetFooter = element.find('#widgetFooter');
    let noChatCloseBtn = false;

    /*
    const MutationObserver = window.MutationObserver || window.WebKitMutationObserver;
    let footerObserver = new MutationObserver((mutationRecords) => {
        console.log("Footer Observer:", mutationRecords);
        noChatCloseBtn = widgetFooter.children().length === 1;
        if(noChatCloseBtn && footerPostButton) footerPostButton.css('position', 'relative');
    });
    footerObserver.observe(widgetFooter.get(0), {childList: true});
    */

    let tags = $('<div id="tags">').appendTo(widgetHeaderWrapper);
    let tabs = $('<nav id="tabs">').appendTo(widgetHeader);
    let innerTabSearch = $('<div id="innerTabSearch">').appendTo(widgetHeader);
    
    let widgetContent = $('<div class="widgetContent">').appendTo(widgetBody);

    let footerPostButton = $('<button class="button button-block" id="postSelected">').prependTo(widgetFooter);
    
    widgetTitle.css('marginBottom', '10px');
    // add a style to translate the remove button of tags!
    $('#smarti-custom-style').remove();
    $(`<style id="smarti-custom-style">.smarti #widgetContainer #widgetWrapper #widgetHeader #tags ul li:after {content:"${Utils.localize({code: 'widget.latch.query.remove'})}"}</style>`).appendTo('head');

    widgetMessage.empty();
    tabs.hide();
    innerTabSearch.hide();
    
    let widgetHeaderTagsTemplate = $.templates(widgetHeaderTagsTemplateStr);
    let widgetHeaderTabsTemplate = $.templates(widgetHeaderTabsTemplateStr);
    let widgetHeaderInnerTabSearchTemplate = $.templates(widgetHeaderInnerTabSearchTemplateStr);
    let widgetFooterPostButtonTemplate = $.templates(widgetFooterPostButtonTemplateStr);
    let widgetConversationTemplate = $.templates(widgetConversationTemplateStr);
    let widgetIrLatchTemplate = $.templates(widgetIrLatchTemplateStr);

    let widgetHeaderTagsTemplateData = {tokens: [], userTokens: []};
    widgetHeaderTagsTemplate.link(tags, widgetHeaderTagsTemplateData);

    let widgetHeaderTabsTemplateData = {widgets: widgets, selectedWidget: 0};
    widgetHeaderTabsTemplate.link(tabs, widgetHeaderTabsTemplateData);

    let widgetHeaderInnerTabSearchTemplateData = {containerTitle: ""};
    widgetHeaderInnerTabSearchTemplate.link(innerTabSearch, widgetHeaderInnerTabSearchTemplateData);

    let widgetFooterPostButtonTemplateData = {title: ""};
    widgetFooterPostButtonTemplate.link(footerPostButton, widgetFooterPostButtonTemplateData);

    let innerTabSearchInput = innerTabSearch.find('input');
    let innerTabSearchSubmit = innerTabSearch.find('#innerTabSearchSubmit');

    //Smarti

    let smarti = new Smarti({
      DDP: {
        endpoint: options.socketEndpoint
      },
      tracker: tracker,
      channel: options.channel
    });//TODO wait for connect?

    smarti.subscribe('smarti.data', function(data) {
        refreshWidgets(data);
    });

    smarti.login(
        initialize,
        drawLogin
    );

    //append lightbulb close (only one handler!)
    let tabOpenButton = $('.flex-tab-container .flex-tab-bar .icon-lightbulb').parent();

    tabOpenButton.unbind('click.closeTracker');

    tabOpenButton.bind('click.closeTracker', function() {
        if($('.external-search-content').is(":visible")) {
            tracker.trackEvent('sidebar.close');
        }
    });

    // widget interaction logic
    var selectionCount = 0;

    widgetBody.scroll(function (event) {
        if (widgetBody.scrollTop() > 1) {
            widgetTitle.slideUp(250);
            if(innerTabSearch.hasClass('active')) innerTabSearch.slideUp(100);
            tabs.addClass('shadow');
        } else {
            widgetTitle.slideDown(200);
            if(innerTabSearch.hasClass('active')) innerTabSearch.slideDown(100);
            tabs.removeClass('shadow');
        }
    });

    let sources = tabs.find('.moreSources');
    tabs.on('click', '.more', function() {
        if (sources.hasClass('open')) {
            sources.slideUp(100).removeClass('open');
            $(this).text(Utils.localize({code: 'smarti.channels'}));
        } else {
            sources.slideDown(200).addClass('open');
            $(this).html('<i class="icon-cancel"></i>');
        }
    });

    sources.on('click', 'li', function() {
        let currentTab = tabs.find('.selected');
        let newTab = $(this);

        if(!newTab.hasClass('selected')) {
            currentTab.removeClass('selected');
            newTab.addClass('selected');
    
            let currentWidget = currentTab.get(0) && $.view(currentTab).data;
            let newWidget = $.view(newTab).data;
    
            if(currentWidget) currentWidget.params.elem.hide();
            newWidget.params.elem.show();
            
            if(newWidget.params.type === "related.conversation") {
                innerTabSearch.removeClass('active');
                innerTabSearch.slideUp(100);
            } else {
                innerTabSearch.addClass('active');
                if(widgetBody.scrollTop() > 1) {
                    innerTabSearch.slideUp(100);
                } else {
                    innerTabSearch.slideDown(100);
                }
            }

            innerTabSearchInput.val("");
            searchTerms = [];
            search(widgetHeaderTabsTemplateData.selectedWidget);
    
            sources.slideUp(100).removeClass('open');
            tabs.find('.more').text(Utils.localize({code: 'smarti.channels'}));

            if(currentWidget) currentWidget.params.elem.find('.selected').removeClass('selected');
            selectionCount = 0;
            footerPostButton.css('transform', 'translateY(200%)');
    
            let newTitle = newTab.text();
    
            $.observable(widgetHeaderTabsTemplateData).setProperty("selectedWidget", $.view(newTab).index);
            $.observable(widgetHeaderInnerTabSearchTemplateData).setProperty("containerTitle", newWidget.params.query.displayTitle);
        }
    });

    widgetBody.on('click', '.smarti-widget .selectMessage', function() {
        let parent = $(this).parent().parent();

        if (parent.hasClass('selected')) {
            parent.removeClass('selected');
            selectionCount--;
        } else {
            parent.addClass('selected');
            selectionCount++;
        }

        if (
            parent.hasClass('parent') &&
            parent.hasClass('selected') &&
            !$(this).closest('.conversation').children('.responseContainer').is(':visible')
        ) {
            $(this).closest('.conversation').children('.responseContainer').toggle(200);
        }

        if(selectionCount === 0) {
            footerPostButton.css('transform', 'translateY(200%)');
        } else {
            $.observable(widgetFooterPostButtonTemplateData).setProperty("title", Utils.localize({code:'widget.conversation.post-selected' + (selectionCount == 1 ? '' : '-all'), args:[selectionCount]}));
            footerPostButton.css('transform', 'translateY(0)');
        }
    });

    widgetBody.on('click', '.convMessage.parent .answers', function() {
        $(this).closest('.conversation').children('.responseContainer').toggle(200);
    });

    function postItems(items) {
        function createTextMessage(text, conv) {
            if(conv.parent.templateType === "related.conversation") {
                text = text + '\n' + conv.parent.content.replace(/\n/g, " ");
            } else {
                text = text + '\n' + '[' + conv.parent.title + '](' + conv.parent.link + '): ' + conv.parent.description;
            }
            $.each(conv.selectedChildIndices, function(i, childIdx) {
                text += createTextMessage('', {parent : conv.parent.answers[childIdx]});
            });
            return text;
        }
        function buildAttachments(conv) {
            let attachment;
            if(conv.parent.templateType === "related.conversation") {
                attachment = {
                    author_name: "\t",
                    author_icon: Utils.getAvatarUrl(conv.parent.userName),
                    text: conv.parent.content,
                    attachments: [],
                    bot: 'assistify',
                    ts: conv.parent.timestamp
                };
                $.each(conv.selectedChildIndices, function(i, childIdx) {
                    attachment.attachments.push(buildAttachments({parent: conv.parent.answers[childIdx]}));
                });
            } else {
                attachment = {
                    title: conv.parent.title,
                    title_link: conv.parent.link,
                    thumb_url: conv.parent.thumb || undefined,
                    text: conv.parent.description
                };
            }

            return attachment;
        }
        items.forEach(conv => {
            let text;
            if(conv.parent.templateType === "related.conversation") {
                text = Utils.localize({code:'widget.conversation.answer.title' + (conv.selectedChildIndices.length ? '' : '_msg')});
            } else {
                text = Utils.localize({code:"widget.latch.answer.title", args:[widgets[widgetHeaderTabsTemplateData.selectedWidget].params.query.displayTitle]});
            }

            if(options.postings && options.postings.type === 'suggestText') {
                messageInputField.post(createTextMessage(text, conv));
            } else if(options.postings && options.postings.type === 'postText') {
                smarti.post(createTextMessage(text, conv),[]);
            } else {
                smarti.post(text, [buildAttachments(conv)]);
            }
        });
    }

    footerPostButton.click(() => {
        let currentWidget = widgets[widgetHeaderTabsTemplateData.selectedWidget];
        if(currentWidget && currentWidget.params.elem) {
            let selectedItems = [];
            currentWidget.params.elem.find('.convMessage, .irl-result').each((idx, item) => {
                let parentMessageData = $.view(item).data;
                let parentIsSelected = $(item).hasClass('selected');
                let conv = {parent: parentMessageData, selectedChildIndices: []};
                if(parentIsSelected) selectedItems.push(conv);
                let responseContainer = $(item).closest('.conversation').children('.responseContainer');
                if(responseContainer.length) {
                    responseContainer.find('.convMessage').each((idx, item) => {
                        let childData = $.view(item).data;
                        let childIndex = $.view(item).index;
                        if($(item).hasClass('selected')) {
                            if(parentIsSelected) {
                                conv.selectedChildIndices.push(childIndex);
                            } else {
                                selectedItems.push({parent: childData, selectedChildIndices: []});
                            }
                        }
                    });
                }
                
            });
            console.log(selectedItems);
            postItems(selectedItems);
        }
    });

    widgetBody.on('click', '.smarti-widget .postAction', function() {
        let selectedItems = [];
        let parent = $(this).parent().parent();
        let parentMessageData = $.view(parent).data;
        let conv = {parent: parentMessageData, selectedChildIndices: parentMessageData.answers ? Array.apply(null, {length: parentMessageData.answers.length}).map(Number.call, Number) : []};
        selectedItems.push(conv);
        console.log(selectedItems);
        postItems(selectedItems);
    });

    let searchTimeout = null;
    let searchTerms = [];
    function search(widgetIdx) {
        if(searchTimeout) clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            searchTimeout = null;

            searchTerms = innerTabSearchInput.val();
            searchTerms = searchTerms.trim().toLowerCase().replace(/\s+/g, ' ');
            searchTerms = searchTerms.length ? searchTerms.split(' ') : [];

            widgets.forEach((w, idx) => {
                if(w && w.params.elem && w.templateType === "ir_latch" && (typeof widgetIdx != "number" || idx == widgetIdx)) {
                    w.getResults(0, idx == widgetHeaderTabsTemplateData.selectedWidget);
                }
            });
        }, 200);
    }

    innerTabSearchSubmit.click(() => {
        search(widgetHeaderTabsTemplateData.selectedWidget);
    });
    innerTabSearchInput.keydown((e) => {
        if(e.which == 13) search(widgetHeaderTabsTemplateData.selectedWidget);
    });

    tags.on('click', 'li:not(".add, .remove")', function() {
        let tokenIds = $.view(this).index;
        let tokenData = $.view(this).data;
        let tokensArray = typeof tokenData === "string" ? widgetHeaderTagsTemplateData.userTokens : widgetHeaderTagsTemplateData.tokens;
        $.observable(tokensArray).remove(tokenIds);
    });

    tags.on('click', 'li.remove', function() {
        $.observable(widgetHeaderTagsTemplateData.userTokens).refresh([]);
        $.observable(widgetHeaderTagsTemplateData.tokens).refresh([]);
    });

    tags.on('click', 'li.add', function() {
        $(this).addClass('active').html(`<input type="text" id="newTagInput" placeholder="${Utils.localize({code: 'smarti.new-search-term'})}">`);
        tags.find('#newTagInput').focus();
    });

    $(tags).keydown(function (e) {
        if (tags.find('li.add').hasClass('active')) {
            if(e.which == 13) {
                var newTag = tags.find('#newTagInput').val().trim();
                if(newTag != "") {
                    if(widgetHeaderTagsTemplateData.tokens.map(t => t.value.toLowerCase()).concat(widgetHeaderTagsTemplateData.userTokens).indexOf(newTag.toLowerCase()) == -1) {
                        $.observable(widgetHeaderTagsTemplateData.userTokens).insert(newTag);
                    }
                    tags.find('li.add').html('+').removeClass('active');
                }
            }
            if(e.which == 13 || e.which == 27) {
                tags.find('li.add').html('+').removeClass('active');
                event.preventDefault();
                event.stopPropagation();
            }
        }
    });

    $.observable(widgetHeaderTagsTemplateData).observeAll(function() {
        search();
    });

    
    $.views.helpers({
        // helper method to highlight text
        hl: (text, noSearchTerms) => {
            if(!text) return text;
            let terms = widgetHeaderTagsTemplateData.tokens.map(t => t.value).concat(widgetHeaderTagsTemplateData.userTokens);
            if(!noSearchTerms) terms = terms.concat(searchTerms);
            terms = [...new Set(terms)]; // unique terms
            terms.forEach(t => {
                text = text.replace(new RegExp(`(${t})`, 'ig'), '<mark>$1</mark>');
            });
            return text;
        }
    });

    return {};
}

// custom new line converter for jsrender/views
$.views.converters("nl", function(val) {
    return val.replace(/\n/g, '<br />');
});

// custom timestamp to local string converter for jsrender/views
$.views.converters("tls", function(val) {
    return moment(val).format(Utils.localize({code: 'smarti.date-format'}));
});

window.SmartiWidget = SmartiWidget;
