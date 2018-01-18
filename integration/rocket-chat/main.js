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
md5 = require('js-md5');
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
    "msg.post.failure": {
        "de": "Nachricht konnte nicht gepostet werden",
        "en": "Posting message failed"
    },
    "get.conversation.params": {
        "de": "Konversations-Parameter konnten nicht geladen werden: $[1]",
        "en": "Cannot load conversation params: $[1]"
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
        "de": "Selektierte Nachrichten posten",
        "en": "Post selected messages"
    }
});

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
            onEvent: (typeof Piwik !== 'undefined' && Piwik) ? Piwik.getTracker().trackEvent : function () {
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

        const numOfRows = wgt_conf.numOfRows || params.query.resultConfig.numOfRows;
        params.elem.hide();
        params.elem.append('<h2>' + params.query.displayTitle + '</h2>');
        let content = $('<div>').appendTo(params.elem);

        function createTermPill(token) {

            return $('<div class="smarti-token-pill">')
                .append($('<span>')
                    .text(
                        Utils.cropLabel(token.value, options.ui.tokenpill.textlength, options.ui.tokenpill.textreplace, options.ui.tokenpill.textreplacemode))
                    ).attr('title', token.value)
                .append('<i class="icon-cancel"></i>')
                .data('token',token)
                .click(function() {
                    $(this).hide();
                    getResults(0);
                    tracker.trackEvent(params.query.creator + ".tag.remove");
                });
        }

        //TODO should be done server side
        function removeDuplicatesBy(keyFn, array) {
            let mySet = new Set();

            return array.filter(function(x) {
                let key = keyFn(x), isNew = !mySet.has(key);
                if (isNew) mySet.add(key);
                return isNew;
            });
        }

        let termPills = $('<div class="smarti-token-pills">').appendTo(content);
        let termPillCancel = $('<div class="smarti-token-pills-remove">').append(
            $('<button>').text(
                Utils.localize({code:'widget.latch.query.remove.all'})
            ).click(function() {
                clearAllTokens();
            })
        );
        termPillCancel.appendTo(content);

        function clearAllTokens() {
            termPills.children().each(function() {
                $(this).hide();
            });
            getResults(0);
            tracker.trackEvent(params.query.creator + ".tag.remove-all");
        }

        /**
         * @param {Object[]} slots
         * @param {Number} slots.tokenIndex
         * @param tokens
         *
         * @returns {Array}
         */
        function perparePillTokens(slots, tokens) {
            let pillTokens = [];
            $.each(slots, function(i, slot) {
                if(slot.tokenIndex !== undefined && slot.tokenIndex > -1) {
                    pillTokens.push(tokens[slot.tokenIndex]);
                } else if(!slot.tokenIndex) {
                    pillTokens.push(slot.token);
                }
            });
            return pillTokens;
        }


        let pillTokens = perparePillTokens(params.slots,params.tokens);

        $.each(removeDuplicatesBy(v=>v.value,pillTokens), function(i,t) {
            termPills.append(createTermPill(t));
        });

        function refresh(data) {

            console.debug('refresh ir-latch search widget:\n', JSON.stringify(data,null,2));

            let tokens = data.tokens;
            let slots = data.templates[params.tempid].slots;
            let pillTokens = perparePillTokens(slots,tokens);

            let reload = false;

            $.each(removeDuplicatesBy(v=>v.value,pillTokens), function(i,t) {
                let contained = false;
                $.each(termPills.children(), function(j,tp) {
                    if($(tp).data('token').value === t.value) {
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

        let inputForm = $('<div class="search-form" role="form"><div class="input-line search"><input type="text" class="search content-background-color" placeholder="Weiter Suchterme" autocomplete="off"> <i class="icon-search secondary-font-color"></i> </div></div>');
        let inputField = inputForm.find('input');

        inputField.keypress(function(e) {
            if(e.which === 13) {
                let val = $(this).val();
                if(val!== undefined && val !== '') {
                    termPills.append(createTermPill({
                        origin:'User',
                        value:val,
                        type:'Keyword'
                    }));
                    $(this).val('');
                    tracker.trackEvent(params.query.creator + '.tag.add');
                }
                getResults(0);
            }
        });

        params.elem.append(inputForm);
        //let resultCount = $('<h3></h3>').appendTo(params.elem);
        //let loader = $('<div class="loading-animation"><div class="bounce1"></div><div class="bounce2"></div><div class="bounce3"></div> </div>').hide().appendTo(params.elem);
        //let results = $('<ul class="search-results">').appendTo(params.elem);
        //let resultPaging = $('<table>').addClass('paging').appendTo(params.elem);

        function getResults(page) {
            let tks = termPills.children(':visible')
                .map(function() {return $(this).data().token.value;})
                .get()
                .join(" ");

            let queryParams = {
                'wt': 'json',
                'fl': '*,score',
                'rows': numOfRows,
                'q':  tks
            };

            //TODO still a hack !!!
            params.query.url = params.query.url.substring(0, params.query.url.indexOf('?')) + '?';

            //append params
            for(let property in params.query.defaults) {
                if (params.query.defaults.hasOwnProperty(property))
                    queryParams[property] = params.query.defaults[property];
            }
            queryParams.start = page > 0 ? (page*numOfRows) : 0;

            //results.empty();
            //resultCount.empty();
            //resultPaging.empty();
            //loader.show();

            // external Solr search
            console.log(`executeSearch ${ params.query.url }, with ${queryParams}`);
            $.ajax({
                url: params.query.url,
                data: queryParams,
                traditional: true,
                dataType: 'jsonp',
                jsonp: 'json.wrf',
                failure: function(err) {
                    console.error({code:'widget.latch.query.failed',args:[params.query.displayTitle,err.responseText]});
                },
                /**
                 *
                 * @param {Object} data
                 * @param {Object} data.response
                 * @param {Number} data.response.numFound
                 *
                 */
                success: function(data) {
                    //loader.hide();

                    tracker.trackEvent(params.query.creator + "",data.response.numFound);

                    if(data.response.numFound === 0) {
                        //resultCount.text(Utils.localize({code:'widget.latch.query.no-results'}));
                    }

                    //params.elem.show();

                    if(!data.response.numFound) return;

                    //map to search results
                    let docs = $.map(data.response.docs, function(doc) {
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
            refresh
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
    

        //params.elem.append('<h2>' + Utils.localize({code:'widget.conversation.title'}) + '</h2>');

        function refresh() {
            getResults(0);
        }

        //let loader = $('<div class="loading-animation"><div class="bounce1"></div><div class="bounce2"></div><div class="bounce3"></div></div>').hide().appendTo(params.elem);
        //let msg = $('<div class="no-result">').appendTo(params.elem);
        //let results = $('<ul class="search-results">').appendTo(params.elem);

        //let resultPaging = $('<table>').addClass('paging').appendTo(params.elem);

        function getResults(page, pageSize) {

            //TODO get remote
            //results.empty();
            //loader.show();
            //resultPaging.empty();

            $.observable(params.templateData).setProperty("loading", true);
            
            let start = pageSize ? page * pageSize : 0;

            smarti.query({
                conversationId: params.id,
                template: params.tempid,
                creator: params.query.creator,
                start: start
            }, function(data) {
                console.log(data);
                //loader.hide();

                $.observable(params.templateData).setProperty("loading", false);
                $.observable(params.templateData.results).refresh(data.docs);
        
                if (data.numFound > 0) {
                    //msg.empty();
                } else {
                    //msg.text(Utils.localize({code: 'widget.conversation.no-results'}));
                    return;
                }

                /**
                 * @param {Object} doc
                 * @param {Object} doc.answers
                 * @param {String} doc.userName
                 * @param {String} doc.content
                 * @param {String} doc.timestamp
                 *
                 * @returns {{author_name: string, author_icon: *, text, attachments: Array, bot: string, ts}}
                 */
                function buildAttachments(doc) {
                    let attachment = {
                        author_name: "\t",
                        author_icon: Utils.getAvatarUrl(doc.userName),
                        text: doc.content,
                        attachments: [],
                        bot: 'assistify',
                        ts: doc.timestamp
                    };

                    $.each(doc.answers, function(i,answer) {
                        attachment.attachments.push(buildAttachments(answer));
                    });

                    return attachment;
                }

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
                            $('<button>').addClass('postAnswer').addClass('button').text(Utils.localize({code:'widget.conversation.post-all',args:[doc.answers.length+1]})).click(function(){
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
                    prev.click(function(){
                        tracker.trackEvent(params.query.creator + ".result.paging", page-1);
                        getResults(page-1,data.pageSize)
                    });
                } else {
                    prev.hide();
                }

                if((data.numFound/data.pageSize) > (page+1)) {
                    next.addClass('active').click(function(){
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
            refresh
        };
    }

    function showError(err) {
        widgetMessage.empty().append($('<p>').text(Utils.localize(err)));
    }

    function drawLogin() {

        widgetContent.empty();

        let form = $('<form><span>Username</span><input type="text"><br><span>Password</span><input type="password"><br><button>Submit</button></form>');

        form.find('button').click(function(){

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
        let uniqueTokens = [...new Set(data.tokens.map(t => t.value))];
        $.observable(widgetHeaderTagsTemplateData.tags).refresh(uniqueTokens);

        let queries = [];
        data.templates.forEach(t => t.queries.forEach(q => queries.push(q)));
        if(!queries.length) {
            tabs.hide();
            //innerTabSearch.hide();
        }
        let firstQueryTitle = queries[0] && queries[0].displayTitle || "";
        $.observable(widgetHeaderInnerTabSearchTemplateData).setProperty("containerTitle", firstQueryTitle);
        $.observable(widgetHeaderTabsTemplateData).setProperty("containerTitle", firstQueryTitle);
        $.observable(widgetHeaderTabsTemplateData.queries).refresh(queries.slice(1));
        if(queries.length) {
            tabs.show();
            //innerTabSearch.show();
        }

        if(!initialized) {
            widgetContent.empty();
            widgetMessage.empty();

            $.each(data.templates, function(i, template){
                $.each(template.queries, function(j, query) {

                    let constructor;

                    switch(template.type) {
                        case 'ir_latch':
                            constructor = IrLatchWidget;break;
                        case 'related.conversation':
                            constructor = ConversationWidget;break;
                    }

                    
                    if(constructor && (!options.widget[query.creator] || !options.widget[query.creator].disabled)) {
                        let elem = $('<div class="smarti-widget">').appendTo(widgetContent);

                        let params = {
                            elem: elem,
                            templateData: {loading: false, results: []},
                            id: data.conversation,
                            slots: template.slots,
                            tempid: i,
                            tokens: data.tokens,
                            query: query
                        };

                        let config = options.widget[query.creator] || {};

                        widgets.push(new constructor(params, config));
                    }

                });
            });

            if(widgets.length > 0) {
                initialized = true;
            } else {
                showError({code:'smarti.result.no-result-yet'});
            }
        } else {
            $.each(widgets, function(i,wgt){
                wgt.refresh(data);
            });
        }
    }

    function initialize() {
        smarti.init(showError);
    }

    const widgetHeaderTagsTemplateStr = `
        <span>Ergebnisse zu:</span>
        <ul>
            <li class="add">+</li>
            {^{for tags}}
            <li>{{:}}</li>
            {{/for}}
            <li class="remove">Alle löschen</li>
        </ul>
    `;
    const widgetHeaderTabsTemplateStr = `
        <div id="tabContainer">
            <span class="nav-item current">{^{:containerTitle}}</span>
            <span class="nav-item more">Kanäle</span>
        </div>
        <ul class="moreSources">
            {^{for queries}}
            <li>{{:displayTitle}}</li>
            {{/for}}
        </ul>
    `;
    const searchIcon = "assets/search.png";
    const widgetHeaderInnerTabSearchTemplateStr = `
        <input type="search" placeholder="" data-link="placeholder{: 'Suchen in [' + containerTitle + ']' }">
        <a href="#" id="innerTabSearchSubmit">
            <img src="${searchIcon}" alt="search">
        </a>
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
                    <div class="message" data-link="class{merge: answers toggle='parent'}">
                        <div class="middle">
                            <div class="datetime">
                                {{tls:timestamp}}
                                {^{if isTopRated}}<span class="topRated">Top</span>{{/if}}
                                {^{if answers}}<span class="answers">{{: answers.length}} Antworten</span>{{/if}}
                            </div>
                            <div class="title"></div>
                            <div class="text"><p>{{nl:content}}</p></div>
                            <div class="postAction">Konversation posten</div>
                            <div class="selectMessage"></div>
                        </div>
                    </div>
                    {^{if answers}}
                        <div class="responseContainer">
                            {^{for answers}}
                                <div class="message">
                                    <div class="middle">
                                        <div class="datetime">
                                        {{tls:timestamp}}
                                        </div>
                                        <div class="title"></div>
                                        <div class="text"><p>{{nl:content}}</p></div>
                                        <div class="postAction">Nachricht posten</div>
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
    
    //Main layout
    element = $(element);

    let widgetHeader = element.find('#widgetHeader');
    let widgetHeaderWrapper = widgetHeader.find('.widgetHeaderWrapper');
    let widgetTitle = widgetHeaderWrapper.find('h4');
    let widgetBody = element.find('#widgetBody');
    let widgetMessage = widgetBody.find('.widgetMessage');
    let widgetFooter = element.find('#widgetFooter');

    let tags = $('<div id="tags">').appendTo(widgetHeaderWrapper);
    let tabs = $('<nav id="tabs">').appendTo(widgetHeader);
    let innerTabSearch = $('<div id="innerTabSearch">').appendTo(widgetHeader);

    let widgetContent = $('<div class="widgetContent">').appendTo(widgetBody);

    let footerPostButton = $('<button class="button button-block" id="postSelected">').prependTo(widgetFooter);

    widgetTitle.css('marginBottom', '30px');

    widgetMessage.empty();
    widgetContent.empty();
    tabs.hide();
    innerTabSearch.hide();
    
    let widgetHeaderTagsTemplate = $.templates(widgetHeaderTagsTemplateStr);
    let widgetHeaderTabsTemplate = $.templates(widgetHeaderTabsTemplateStr);
    let widgetHeaderInnerTabSearchTemplate = $.templates(widgetHeaderInnerTabSearchTemplateStr);
    let widgetFooterPostButtonTemplate = $.templates(widgetFooterPostButtonTemplateStr);
    let widgetConversationTemplate = $.templates(widgetConversationTemplateStr);

    let widgetHeaderTagsTemplateData = {tags: []};
    widgetHeaderTagsTemplate.link(tags, widgetHeaderTagsTemplateData);

    let widgetHeaderTabsTemplateData = {queries: [], containerTitle: ""};
    widgetHeaderTabsTemplate.link(tabs, widgetHeaderTabsTemplateData);

    let widgetHeaderInnerTabSearchTemplateData = {containerTitle: ""};
    widgetHeaderInnerTabSearchTemplate.link(innerTabSearch, widgetHeaderInnerTabSearchTemplateData);

    let widgetFooterPostButtonTemplateData = {title: ""};
    widgetFooterPostButtonTemplate.link(footerPostButton, widgetFooterPostButtonTemplateData);

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
            //innerTabSearch.slideUp(100);
            tabs.addClass('shadow');
        }
        else {
            widgetTitle.slideDown(200);
            //innerTabSearch.slideDown(100);
            tabs.removeClass('shadow');
        }
    });

    let sources = tabs.find('.moreSources');
    tabs.on('click', '.more', function () {
        if (sources.hasClass('open')) {
            sources.slideUp(100).removeClass('open');
            $(this).html("Kanäle");
        } else {
            sources.slideDown(200).addClass('open');
            $(this).html(" <svg width=\"8px\" height=\"8px\" class=\"close\" viewBox=\"0 0 8 8\" version=\"1.1\"\n" +
                "                     xmlns=\"http://www.w3.org/2000/svg\"\n" +
                "                     xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n" +
                "                    <!-- Generator: Sketch 48.1 (47250) - http://www.bohemiancoding.com/sketch -->\n" +
                "                    <desc>Created with Sketch.</desc>\n" +
                "                    <defs></defs>\n" +
                "                    <g id=\"Page-1\" stroke=\"none\" stroke-width=\"1\" fill=\"none\" fill-rule=\"evenodd\">\n" +
                "                        <g id=\"Desktop-HD\" transform=\"translate(-1343.000000, -194.000000)\">\n" +
                "                            <image id=\"np_multiply_1156273_000000\" x=\"1341\" y=\"192\" width=\"12\" height=\"12\"\n" +
                "                                   xlink:href=\"data:image/png;base64,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\"></image>\n" +
                "                        </g>\n" +
                "                    </g>\n" +
                "                </svg>");
        }
    });

    sources.on('click', 'li', function () {
        let tabsCurrent = tabs.find('.current');
        var newSelection = $(this).text();
        var currentSelection = tabsCurrent.text();
        $(this).text(currentSelection);
        //tabsCurrent.text(newSelection);
        sources.slideUp(100).removeClass('open');
        tabs.find('.more').text("Kanäle");
        //innerTabSearch.find('input').attr('placeholder', 'Suchen in "'+newSelection.split(' (')[0]+'\"');

        $.observable(widgetHeaderInnerTabSearchTemplateData).setProperty("containerTitle", newSelection);
        $.observable(widgetHeaderTabsTemplateData).setProperty("containerTitle", newSelection);
    });

    widgetBody.on('click', '.message .selectMessage', function () {
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
            $.observable(widgetFooterPostButtonTemplateData).setProperty("title", Utils.localize({code:'widget.conversation.post-selected' + (selectionCount == 1 ? '' : '-all')}));
            footerPostButton.css('transform', 'translateY(0)');
        }
    });

    widgetBody.on('click', '.message.parent .answers', function () {
        $(this).closest('.conversation').children('.responseContainer').toggle(200);
    });

    tags.on('click', 'li:not(".add, .remove")', function () {
        $(this).remove();
    });

    tags.on('click', 'li.remove', function () {
        tags.find('li').not('.add, .remove').remove();
    });

    tags.on('click', 'li.add', function () {
        $(this).addClass('active').html('<input type="text" id="newTagInput" placeholder="New tag">');
        tags.find('#newTagInput').focus();
    });

    $(tags).keydown(function (e) {
        if (tags.find('li.add').hasClass('active')) {
            if(e.which == 13) {
                var newTag = tags.find('#newTagInput').val();
                if(newTag != "") {
                    if(tags.find('li').not('.add, .remove').map((idx, item) => $(item).text()).get().indexOf(newTag) == -1) {
                        tags.find('ul').children('li.add').after('<li>' + newTag + '</li>');
                    }
                    tags.find('li.add').html('+').removeClass('active');
                }
            }
            if(e.which == 13 || e.which == 27) {
                tags.find('li.add').html('+').removeClass('active');
            }
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
    return (new Date(val)).toLocaleString();
});

window.SmartiWidget = SmartiWidget;
