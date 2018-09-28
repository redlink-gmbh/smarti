/*
 * Copyright 2018 Redlink GmbH
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
const ld_lang = require('lodash/lang');
const toastr = require('toastr');
require('./node_modules/toastr/toastr.scss');
require('jsviews');

const clipboard = require('./clipboard');

const DDP = require("ddp.js").default;

let conversationId = null;

let searchProvider = null;
let searchProviderSupported = false;

//multi-linguality
const Localize = require('localize');
const i18n = require('./i18n.json');
let localize = new Localize(i18n, undefined, 'xx');
toastr.options.target = ".widgetMessage";
toastr.options.positionClass = "widgetToast";

let Logger = function(prefix) {
    this.logger = {};
    for (let m in console)
        if(typeof console[m] == 'function')
            this.logger[m] = console[m].bind(window.console, prefix + ":");

    return this.logger;
};

let log = Logger("SmartiWidget");

const Utils = {
    getAvatarUrl : (id) => {
        return "https://www.gravatar.com/avatar/"+md5('redlink'+id)+"?d=identicon";
    },
    getAnonymUser: (id) => {
        return 'User-' + (parseInt(md5('redlink'+id),16)%10000); //TODO check if this works somehow...
    },
    localize: (obj) => {
        if(obj.args && obj.args.length > 0) {
            const args_clone = ld_lang.cloneDeep(obj.args);
            args_clone.unshift(obj.code);
            return localize.translate.apply(null,args_clone);
        }
        return localize.translate(obj.code);
    },
    mapDocType: (doctype) => {
        switch(doctype) {
            case 'application/xhtml+xml': return 'html';
        }
        return doctype.substring(doctype.indexOf('/')+1).slice(0,4);
    },
    cropLabel: (label,max_length,replace,mode) => {
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

    options = $.extend(true, {
        DDP: {
            SocketConstructor: WebSocket
        },
        tracker: new Tracker()
    }, options);

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
            ddp.on('result', (message) => {
                if (message.id === loginId) {
                    if (message.error) {
                        log.error("Login error:", message.error);
                        return failure({i18nObj: {code:"login.failed", args:[message.error.reason]}});
                    }
                    localStorage.setItem('Meteor.loginToken', message.result.token);
                    localStorage.setItem('Meteor.loginTokenExpires', new Date(message.result.tokenExpires.$date));
                    localStorage.setItem('Meteor.userId', message.result.id);
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
            log.debug(`found token ${localStorage.getItem('Meteor.loginToken')} for user ${localStorage.getItem('Meteor.userId')} that expires on ${localStorage.getItem('Meteor.loginTokenExpires')}`);

            loginRequest([
                { "resume": localStorage.getItem('Meteor.loginToken') }
            ]);

        } else {
            failure({i18nObj: {code:'login.no-auth-token'}});
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

        // get RC's search provider
        const spCallId = ddp.method("rocketchatSearch.getProvider", []);
        ddp.on("result", (message) => {
            if(message.id === spCallId) {
                if (message.error) {
                    log.error('Failed to get search provider:', message.error);
                    return;
                } else {
                    if(message.result) {
                        searchProvider = message.result;
                        log.debug('Search provider found: ', searchProvider);
                        if(searchProvider.key == "chatpalProvider") {
                            searchProviderSupported = true;
                        } else {
                            searchProviderSupported = false;
                            log.info(`Search provider ${searchProvider.key} is not supported!`);
                        }
                    } else {
                        log.info('Search provider is not active!');
                        return;
                    }
                }
            }
        });

        // fetch last Smarti results when the wiget gets initialized (channel entered)
        log.debug('init -> get conversation ID for channel', options.channel);
        const lastConvCallId = ddp.method("getConversationId", [options.channel]);
        ddp.on("result", (message) => {
            if(message.id === lastConvCallId) {
                if (message.error) {
                    log.error('Failed to get conversation ID:', message.error);
                    return failure({i18nObj: {code: 'smarti.result.conversation-not-found'}});
                } else {
                    if (message.result) {
                        // conversation ID found for channel -> fetch conversation results
                        conversationId = message.result;
                        getConversation(message.result, failure);
                    } else {
                        log.debug('init -> conversation ID not found for channel:', options.channel);
                        return failure({i18nObj: {code: 'smarti.result.conversation-not-found'}});
                    }
                }
            }
        });

        // subscribe changes on the channel that is passed by SmartiWidget constructor (message send)
        log.debug('init -> subscribe channel', options.channel);
        const subId = ddp.sub("stream-notify-room", [options.channel+"/newConversationResult", false]);
        ddp.on("nosub", () => {
            failure({i18nObj: {code:'sub.new-conversation-result.nosub'}, dismissOnClick: true});
        });
        ddp.on("changed", (message) => {
            if(message.collection == "stream-notify-room") {
                // subscriotion has changed (message send) -> fetch conversation results
                log.debug('Async smarti results received:', message.fields.args[0]);
                pubsub('smarti.data').publish(message.fields.args[0]);
            }
        });
    }

    function refresh() {
        if(conversationId) {
            getConversation(conversationId, showError);
        } else {
            Console.warn("Widget data refresh failed. Conversation ID not found!");
        }
    }

    /**
     * Fetches the analyzed conversation by Id
     *
     * @param conversationId - the conversation Id to get the Smarti results for
     * @param failure - a function called on any errors to display a message
     */
    function getConversation(conversationId, failure) {
        log.debug('Fetch results for conversation with ID:', conversationId);
        const msgid = ddp.method("getConversation", [conversationId]);
        ddp.on("result", (message) => {
            if(message.id === msgid) {
                if (message.error) {
                    log.error('Failed to get conversation:', message.error);
                    if (failure) failure({i18nObj: {code: 'smarti.result.conversation-not-found'}});
                } else {
                    if (message.result && message.result != "null") {
                        if (message.result.error) {
                            log.error('Server-side error:', message.result.error);
                            //const errorCode = message.result.error.code || message.result.error.response && message.result.error.response.statusCode;
                            if (failure) failure({i18nObj: {code: 'smarti.result.conversation-not-found'}});
                        } else {
                            pubsub('smarti.data').publish(message.result);
                        }
                    } else {
                        log.info(`Conversation fetch returned no results. Expecting async response... (${conversationId})`);
                    }
                }
            }
        });
    }

    function query(params, success, failure) {
        log.debug(`Get query builder result for conversation with [id: ${params.conversationId}, templateIndex: ${params.template}, creatorName: ${params.creator}, start: ${params.start}}`);
      const msgid = ddp.method("getQueryBuilderResult",[params.conversationId, params.template, params.creator, params.start]);
      ddp.on("result", (message) => {
          if(message.id === msgid) {
              if (message.error) {
                  log.error("Query error:", message.error);
                  return failure({i18nObj: {code: "get.query.params", args: [message.error.reason]}, dismissOnClick: true});
              }
              success(message.result || {});
          }
      });
    }

    function search(params, success, failure) {
        log.info('Searching for conversation messages...');
        const msgid = ddp.method("searchConversations",[params]);
        ddp.on("result", (message) => {
            if(message.id === msgid) {
                if (message.error) {
                    log.error("Conversation search error:", message.error);
                    failure({i18nObj: {code:'smarti.result.conversation-not-found'}, dismissOnClick: true});
                } else if (!message.result || message.result.error) {
                    if(!message.result) {
                        log.info("Conversation search returned no result!");
                    } else {
                        log.error("Conversation search error:", message.result.error);
                    }
                    failure({i18nObj: {code:'smarti.result.conversation-not-found'}, dismissOnClick: true});
                } else {
                    success(message.result);
                }
            }
        });
    }

    function rcSearch(params, success, failure) {
        log.debug('RC Search...', params);
        const msgid = ddp.method("rocketchatSearch.search", params);
        ddp.on("result", (message) => {
            if(message.id === msgid) {
                if (message.error) {
                    log.error("RC search error:", message.error);
                    failure({i18nObj: {code:'smarti.result.conversation-not-found'}, dismissOnClick: true});
                } else if (!message.result || message.result.error) {
                    if(!message.result) {
                        log.info("RC search returned no result!");
                    } else {
                        log.error("RC search error:", message.result.error);
                    }
                    failure({i18nObj: {code:'smarti.result.conversation-not-found'}, dismissOnClick: true});
                } else {
                    success(message.result);
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
    function post(msg, attachments, success, failure) {
        const methodId = ddp.method("sendMessage",[{rid:options.channel, msg:msg, attachments:attachments, origin:'smartiWidget'}]);
        ddp.on("result", (message) => {
            if(message.id === methodId) {
                if(message.error) {
                    log.error('Cannot post message:', message.error);
                    if(failure) failure({i18nObj: {code:"msg.post.failure"}, dismissOnClick: true});
                } else if(success) {
                    success();
                }
            }
        });
    }

    return {
        login,
        init,
        refresh,
        subscribe: (id, func) => pubsub(id).subscribe(func),
        unsubscribe: (id, func) => pubsub(id).unsubscribe(func),
        query,
        search,
        rcSearch,
        post
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
    this.trackEvent = (action, value) => {
        log.debug(`track event: ${category}, ${action}, ${roomId}, ${value}`);
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
            onEvent: (typeof Piwik !== 'undefined' && Piwik) ? Piwik.getTracker().trackEvent : () => {
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
        lang: 'de',
        i18n: i18n
    };

    $.extend(true,options,_options);

    // log.debug('init:', options);

    localize = new Localize(options.i18n, undefined, 'xx');
    localize.setLocale(options.lang);

    moment.locale(options.lang);

    let tracker = new Tracker(options.tracker.category,options.channel,options.tracker.onEvent);

    let widgets = [];

    let messageInputField;

    function InputField(elem) {
        this.post = (msg) => {
            log.debug(`write text to element: ${msg}`);
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

        let lastTks = [];
        let currentPage = 0;
        let loadedPage = 0;
        let noMoreData = false;

        function refresh() {
            getResults(0, false, false);
        }

        function getResults(page, useSearchTerms, append) {

            let tks = widgetHeaderTagsTemplateData.tokens.map(t => t.value).concat(widgetHeaderTagsTemplateData.userTokens);
            if(useSearchTerms) tks = tks.concat(searchTerms || []);

            $.observable(params.templateData).setProperty("noTags", !tks.length);

            if(equalArrays(lastTks, tks) && loadedPage >= page) return;

            if(!append) {
                page = 0;
                currentPage = 0;
                noMoreData = false;
            }

            lastTks = tks;

            let queryParams = {
                'wt': 'json',
                'fl': '*,score',
                'rows': numOfRows,
                'q':  getSolrQuery(tks)
            };

            params.query.url = params.query.url.substring(0, params.query.url.indexOf('?')) + '?';

            //append params
            for(let property in params.query.defaults) {
                if (params.query.defaults.hasOwnProperty(property))
                    queryParams[property] = params.query.defaults[property];
            }
            queryParams.start = page > 0 ? (page*numOfRows) : 0;

            // external Solr search
            log.debug(`executeSearch ${ params.query.url }, with`, queryParams);
            $.observable(params.templateData).setProperty("loading", true);
            $.ajax({
                url: params.query.url,
                data: queryParams,
                traditional: true,
                dataType: 'jsonp',
                jsonp: 'json.wrf',
                failure: (err) => {
                    log.error(Utils.localize({code:'widget.latch.query.failed', args:[params.query.displayTitle, err.responseText]}));
                },
                /**
                 *
                 * @param {Object} data
                 * @param {Object} data.response
                 * @param {Number} data.response.numFound
                 *
                 */
                success: (data) => {
                    tracker.trackEvent(params.query.creator, data.response && data.response.docs && data.response.docs.length || 0);

                    loadedPage = page;
                    noMoreData =    !data.response ||
                                    !data.response.docs ||
                                    !data.response.docs.length ||
                                    (params.templateData.results.length + data.response.docs.length) == data.response.numFound;

                    log.debug("ir-latch query:", params.query);
                    log.debug("ir-latch response:", data.response);

                    //map to search results
                    let docs = $.map(data.response && data.response.docs || [], (doc) => {
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

                    if(docs && docs.length) {
                        docs.forEach(d => {
                            d.templateType = "ir_latch";
                        });
                    }

                    $.observable(params.templateData).setProperty("total", data.response && data.response.numFound || 0);

                    if(append) {
                        $.observable(params.templateData.results).insert(docs);
                    } else {
                        $.observable(params.templateData.results).refresh(docs);
                    }
                    $.observable(params.templateData).setProperty("loading", false);

                    if(params.elem.height() <= widgetBody.innerHeight()) {
                        if(params.elem.prevAll().length == widgetHeaderTabsTemplateData.selectedWidget) widgetFooter.removeClass('shadow');
                        loadNextPage();
                    } else {
                        if(params.elem.prevAll().length == widgetHeaderTabsTemplateData.selectedWidget) widgetFooter.addClass('shadow');
                    }
                }
            });
        }

        function loadNextPage() {
            if(!noMoreData) {
                log.debug("Load more:", params.query.creator);
                currentPage++;
                getResults(currentPage, true, true);
            }
        }

        getResults(currentPage);

        return {
            params,
            refresh,
            getResults,
            loadNextPage,
            templateType: "ir_latch",
            queryCreator: params.query.creator.split(":").slice(0, 2).join(":")
        };
    }

    let analyzedMessages = 0;

    /**
     * @param params
     * @returns {Object} {
     *      refresh: FUNCTION
     * }
     * @constructor
     */
    function ConversationWidget(params) {
        widgetConversationTemplate.link(params.elem, params.templateData);
        $.observable(params.templateData.filters).observeAll(onDataChange);

        let lastSimilarityQuery = null;
        let currentSimilarityQuery = null;
        let lastTks = [];
        let lastQueryTerms = "";
        let lastContext = {};
        let lastFilters = [];
        let currentFilters = [];
        let currentPage = 0;
        let loadedPage = 0;
        let noMoreData = false;

        function refresh() {
            getResults(0, false, false);
        }

        function getResults(page, useSearchTerms, append) {

            if(params.query.creator.indexOf("queryBuilder:rocketchatsearch") > -1) {

                if(!searchProvider) {
                    return $.observable(params.templateData).setProperty("msg", Utils.localize({code: 'rc.search-provider.inactive'}));
                }

                if(!searchProviderSupported) {
                    return $.observable(params.templateData).setProperty("msg", Utils.localize({code: 'rc.search-provider.not-supported'}));
                }

                let queryTerms = (params.query.contextQuery||[]).sort((a, b) => b.relevance - a.relevance).map((q) => q.term);

                let tks = widgetHeaderTagsTemplateData.tokens.filter(t => t.pinned).map(t => t.value).concat(widgetHeaderTagsTemplateData.userTokens);
                // if(useSearchTerms) tks = tks.concat(searchTerms || []);

                queryTerms = tks.length ? tks : queryTerms;

                currentSimilarityQuery = params.query.similarityQuery;

                let context = {"uid": Meteor.userId(), "rid": Session.get("openedRoom")};

                if(lastSimilarityQuery === currentSimilarityQuery && equalArrays(lastQueryTerms, queryTerms) && context.uid == lastContext.uid && context.rid == lastContext.rid && loadedPage >= page) return;

                if(!append) {
                    page = 0;
                    currentPage = 0;
                    noMoreData = false;
                }

                let pageSize = params.query.payload && params.query.payload.rows || 0;
                let start = pageSize ? page * pageSize : 0;

                $.observable(params.templateData).setProperty("loading", true);

                lastQueryTerms = queryTerms;

                let payload = JSON.parse(JSON.stringify(params.query.payload));

                payload.start = start;
                payload.rows = pageSize;

                // SimilarityQuery
                lastSimilarityQuery = currentSimilarityQuery;
                if (currentSimilarityQuery) {
                    payload["q.alt"] = currentSimilarityQuery;
                } else {
                    delete payload["q.alt"];
                }

                lastContext = context;

                smarti.rcSearch([queryTerms.join(" "), context, payload], (data) => {
                    log.debug("RC search results:", data);

                    loadedPage = page;

                    let messages = [];
                    data = data.message;
                    if(data.docs && data.docs.length) {
                        data.docs.forEach(m => {
                            /* default provider message
                            messages.push({
                                content: m.msg,
                                time: m._updatedAt ? m._updatedAt.$date : m.ts.$date,
                                username: m.u.name
                            });
                            */

                            let user = Meteor.users.findOne({username: m.username});
                            let userDisplayName = user ? user.name : m.username;

                            messages.push({
                                _id: m._id,
                                rid: m.rid,
                                messageLink: getRCMessageLink(m.rid, m._id),
                                content: m.text[0],
                                time: m.created,
                                username: m.username,
                                userDisplayName: userDisplayName,
                                roomType: m.r.t,
                                roomName: m.r.name
                            });

                            /* Chatpal results message
                                {
                                    "rid":"DFGNe9uuLts4882yF",
                                    "user":"ur6BtsL5grm6BfinF",
                                    "created":"2018-05-14T17:20:40.707Z",
                                    "type":"message",
                                    "score":15.663331,
                                    "_id":"5xoJQpoySBgHkbYEw",
                                    "text":[
                                        "Content <em>string</em>"
                                    ],
                                    "r":{
                                        "name":"room-name",
                                        "t":"r"
                                    },
                                    "username":"username",
                                    "valid":true
                                }
                             */
                        });
                    }

                    tracker.trackEvent(params.query.creator, messages.length);

                    noMoreData = !data.docs || !data.docs.length || typeof data.numFound == "undefined" || (params.templateData.results.length + data.docs.length) == data.numFound;

                    if(typeof data.numFound != "undefined") $.observable(params.templateData).setProperty("total", data.numFound);

                    if(append) {
                        $.observable(params.templateData.results).insert(messages);
                    } else {
                        $.observable(params.templateData.results).refresh(messages);
                    }
                    $.observable(params.templateData).setProperty("tokensUsed", queryTerms.length > 0);
                    $.observable(params.templateData).setProperty("noMessages", analyzedMessages <= 0);
                    $.observable(params.templateData).setProperty("loading", false);

                    if(params.elem.height() <= widgetBody.innerHeight()) {
                        if(params.elem.prevAll().length == widgetHeaderTabsTemplateData.selectedWidget) widgetFooter.removeClass('shadow');
                        loadNextPage();
                    } else {
                        if(params.elem.prevAll().length == widgetHeaderTabsTemplateData.selectedWidget) widgetFooter.addClass('shadow');
                    }
                }, function(error) {
                    if(error) {
                        showError(error);
                    } else {
                        showMsg({i18nObj: {code: 'smarti.result.no-response'}, isError: true, dismissOnClick: true});
                    }
                    $.observable(params.templateData).setProperty("loading", false);
                });
            } else if(params.query.creator.indexOf("queryBuilder:conversationsearch") > -1) {
                /*
                    fl:"id,message_id,meta_channel_id,user_id,time,message,type"
                    hl:"true"
                    hl.fl:"message"
                    rows:3
                    sort:"time desc"
                */
                let tks = widgetHeaderTagsTemplateData.tokens.filter(t => t.pinned).map(t => t.value).concat(widgetHeaderTagsTemplateData.userTokens);
                if(useSearchTerms) tks = tks.concat(searchTerms || []);

                currentSimilarityQuery = params.query.similarityQuery;

                currentFilters = [];
                params.query.filterQueries.forEach(fq => {
                    if(!fq.optional) {
                        currentFilters.push(fq.filter);
                    } else {
                        let enabled = widgetStorage.widgetOptions && widgetStorage.widgetOptions[params.query.creator] && widgetStorage.widgetOptions[params.query.creator].filters[fq.filter];
                        if(typeof enabled == "undefined") enabled = fq.enabled;
                        if(enabled) {
                            currentFilters.push(fq.filter);
                        }
                    }
                });

                if(lastSimilarityQuery === currentSimilarityQuery && equalArrays(lastTks, tks) && equalArrays(lastFilters, currentFilters) && loadedPage >= page) return;

                if(!append) {
                    page = 0;
                    currentPage = 0;
                    noMoreData = false;
                }

                let pageSize = params.query.defaults && params.query.defaults.rows || 0;
                let start = pageSize ? page * pageSize : 0;

                $.observable(params.templateData).setProperty("loading", true);

                lastTks = tks;

                let queryParams = {};

                //set default params
                for(let property in params.query.defaults) {
                    if (params.query.defaults.hasOwnProperty(property))
                        queryParams[property] = params.query.defaults[property];
                }

                // Filters
                queryParams.fq = lastFilters = currentFilters;
                queryParams.start = start;
                queryParams.q = getSolrQuery(tks);

                // SimilarityQuery
                lastSimilarityQuery = currentSimilarityQuery;
                if (currentSimilarityQuery) {
                    queryParams["q.alt"] = currentSimilarityQuery;
                } else {
                    delete queryParams["q.alt"];
                }
                log.debug("queryParams:", queryParams);

                smarti.search(queryParams, (data) => {
                    log.debug("Conversation search results:", data);

                    loadedPage = page;

                    let conversations = [];
                    if(data.docs && data.docs.length) {
                        data.docs.forEach(d => {
                            d.results.forEach(r => {
                                if(r.messages.length) {
                                    let conversation = r.messages[0];
                                    conversation.cid = d.id;
                                    r.messages.slice(1).forEach(m => {
                                        conversation.content += '\r\n' + m.content;
                                    });
                                    conversation.messagesAfter = r.after || [];
                                    conversation.messagesBefore = r.before || [];

                                    conversation.messagesCnt = conversation.messagesBefore.length + conversation.messagesAfter.length;

                                    conversation.templateType = "related.conversation";
                                    conversation.messagesAfter.forEach(m => {
                                        m.templateType = "related.conversation";
                                    });
                                    conversation.messagesBefore.forEach(m => {
                                        m.templateType = "related.conversation";
                                    });


                                    conversations.push(conversation);
                                }
                            });
                        });
                    }

                    tracker.trackEvent(params.query.creator, conversations.length);

                    noMoreData = !data.docs || !data.docs.length || (params.templateData.results.length + data.docs.length) == data.numFound;

                    if(typeof data.numFound != "undefined") $.observable(params.templateData).setProperty("total", data.numFound);

                    if(append) {
                        $.observable(params.templateData.results).insert(conversations);
                    } else {
                        $.observable(params.templateData.results).refresh(conversations);
                    }
                    $.observable(params.templateData).setProperty("tokensUsed", tks.length > 0);
                    $.observable(params.templateData).setProperty("noMessages", analyzedMessages <= 0);
                    $.observable(params.templateData).setProperty("loading", false);

                    if(params.elem.height() <= widgetBody.innerHeight()) {
                        if(params.elem.prevAll().length == widgetHeaderTabsTemplateData.selectedWidget) widgetFooter.removeClass('shadow');
                        loadNextPage();
                    } else {
                        if(params.elem.prevAll().length == widgetHeaderTabsTemplateData.selectedWidget) widgetFooter.addClass('shadow');
                    }
                }, function(error) {
                    if(error) {
                        showError(error);
                    } else {
                        showMsg({i18nObj: {code: 'smarti.result.no-response'}, isError: true, dismissOnClick: true});
                    }
                    $.observable(params.templateData).setProperty("loading", false);
                });
            } else if(params.query.creator.indexOf("queryBuilder:conversationmlt") > -1) {

                $.observable(params.templateData).setProperty("msg", Utils.localize({code: 'smarti.source.not-supported'}));
                return; // conversationmlt not supported anymore!

                if(!append) {
                    page = 0;
                    currentPage = 0;
                    noMoreData = false;
                }

                let pageSize = params.query.defaults && params.query.defaults.rows || 0;
                let start = pageSize ? page * pageSize : 0;

                $.observable(params.templateData).setProperty("loading", true);

                smarti.query({
                    conversationId: params.id,
                    template: params.tempid,
                    creator: params.query.creator,
                    start: start,
                    rows: pageSize
                }, (data) => {

                    tracker.trackEvent(params.query.creator, data.docs && data.docs.length);

                    loadedPage = page;
                    noMoreData = !data.docs || !data.docs.length || (params.templateData.results.length + data.docs.length) == data.numFound;

                    if(data.docs && data.docs.length) {
                        data.docs.forEach(d => {
                            d.templateType = "related.conversation";
                            d.answers.forEach(a => {
                                a.templateType = "related.conversation";
                            });
                        });
                    }

                    if(append) {
                        $.observable(params.templateData.results).insert(data.docs);
                    } else {
                        $.observable(params.templateData.results).refresh(data.docs);
                    }

                    $.observable(params.templateData).setProperty("loading", false);

                }, function(error) {
                    if(error) {
                        showError(error);
                    } else {
                        showMsg({i18nObj: {code: 'smarti.result.no-response'}, isError: true, dismissOnClick: true});
                    }
                    $.observable(params.templateData).setProperty("loading", false);
                });
            }
        }

        function loadNextPage() {
            if(!noMoreData) {
                log.info("Load more:", params.query.creator);
                currentPage++;
                getResults(currentPage, true, true);
            }
        }

        getResults(currentPage);

        return {
            params,
            refresh,
            getResults,
            loadNextPage,
            templateType: "related.conversation",
            queryCreator: params.query.creator.split(":").slice(0, 2).join(":")
        };
    }

    function showMsg(options) {
        const toastrFunction = options.isError ? toastr.error : toastr.info;
        toastrFunction(options.i18nObj ? Utils.localize(options.i18nObj) : options.msg, null, {timeOut: 0, extendedTimeOut: 0, tapToDismiss: options.dismissOnClick || false});
    }

    function showError(options) {
        options.isError = true;
        showMsg(options);
    }

    function drawLogin() {

        widgetContent.empty();

        let form = $('<form><span>Username</span><input type="text"><br><span>Password</span><input type="password"><br><button>Submit</button></form>');

        form.find('button').click(() => {

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
        let urls = data.tokens
            .filter(t => t.hints && t.hints.indexOf("entity.type.url") > -1)
            .map(t => t.value.toLowerCase());
        let tokens = data.tokens
            // #206 - show all tokens .filter(t => t.type !== "Attribute")
            .filter(t => !t.hints || t.hints.indexOf("entity.type.url") === -1)
            .filter(t => urls.indexOf(t.value.toLowerCase()) === -1)
            .sort((a, b) => {
                return a.messageIdx - b.messageIdx;
            })
            .reverse();

        let filteredTokens = {};
        let uniqueTokens = tokens
            .filter((t) => {
                if(filteredTokens[t.value]) return false;
                filteredTokens[t.value] = true;
                return true;
            })
            .reverse()
            .slice(-7);

        uniqueTokens = uniqueTokens
            .filter(t => {
                return widgetHeaderTagsTemplateData.exclude.indexOf(t.value.trim().toLowerCase()) === -1;
            })
            .filter(t => {
                return !widgetHeaderTagsTemplateData.include.some(iT => iT.value.trim().toLowerCase() === t.value.trim().toLowerCase());
            });

        log.debug("Filtered tokens:", uniqueTokens);

        if(widgetHeaderTagsTemplateData.include.length) {
            log.debug("Pinned tokens:", widgetHeaderTagsTemplateData.include);
            uniqueTokens = widgetHeaderTagsTemplateData.include.map(t => {
                t.pinned = true;
                return t;
            }).concat(uniqueTokens);
        }

        if(data.context) {
            analyzedMessages = data.context.end - data.context.start - data.context.skipped;
        }

        if(!initialized) {
            widgetContent.empty();
            widgetMessage.empty();

            $.each(data.templates, (i, template) => {
                $.each(template.queries, (j, query) => {

                    let constructor;

                    switch(template.type) {
                        case 'ir_latch':
                            constructor = IrLatchWidget;break;
                        case 'related.conversation':
                            constructor = ConversationWidget;break;
                    }

                    const isRCSearch = query.creator.indexOf("queryBuilder:rocketchatsearch") > -1;
                    const isConversationMlt = query.creator.indexOf("queryBuilder:conversationmlt") > -1;

                    if(
                        constructor &&
                        (!options.widget[query.creator] || !options.widget[query.creator].disabled) &&
                        !isConversationMlt
                    ) {
                        let elem = $('<div class="smarti-widget">').hide().appendTo(widgetContent);

                        let params = {
                            elem: elem,
                            templateData: {loading: false, results: [], total: 0, msg: "", tokensUsed: false, noMessages: false, isRCSearch},
                            id: data.conversation,
                            slots: template.slots,
                            type: template.type,
                            tempid: i,
                            tokens: data.tokens,
                            query: query
                        };

                        if(template.type == "related.conversation") {
                            // filterQueries
                            if(query.filterQueries) {
                                params.templateData.filters = query.filterQueries.filter(fq => {
                                    return fq.optional;
                                });
                                params.templateData.filters.forEach(fq => {
                                    let enabled = widgetStorage.widgetOptions && widgetStorage.widgetOptions[params.query.creator] && widgetStorage.widgetOptions[params.query.creator].filters[fq.filter];
                                    fq.enabled = typeof enabled == "undefined" ? fq.enabled : enabled;
                                    try {
                                        fq.label = Utils.localize({code: fq.name});
                                    } catch(e) {
                                        let nameParts = fq.name.split(".");
                                        fq.label = nameParts[nameParts.length - 1];
                                    }
                                    if(fq.name !== "filter.property.support_area") {
                                        fq.label += ": " + (fq.displayValue || fq.filter);
                                    }
                                });
                            }
                        }

                        let config = options.widget[query.creator] || {};

                        if(isRCSearch) {
                            $.observable(widgets).insert(0, new constructor(params, config));
                        } else {
                            $.observable(widgets).insert(new constructor(params, config));
                        }
                    }

                });
            });

            if(widgets.length > 0) {
                initNavTabs();
                initialized = true;
            } else {
                showMsg({i18nObj: {code:'smarti.no-widgets'}});
            }
        } else {
            $.each(widgets, (i, wgt) => {
                // update widget queries
                $.each(data.templates, (i, template) => {
                    $.each(template.queries, (j, query) => {
                        if(wgt.params.query.creator == query.creator) {
                            $.observable(wgt.params).setProperty("query", query);
                        }
                    });
                });
                wgt.refresh();
            });
        }

        // triggers search
        $.observable(widgetHeaderTagsTemplateData.tokens).refresh(uniqueTokens);
    }

    function initialize() {
        smarti.init(showError);
    }

    function initNavTabs() {
        tabs.show();
        tabs.find('.sources li').first().click();
    }

    let widgetStorage = {
        tokens: {
            userTokens: [],
            include: [],
            exclude: []
        },
        widgetOptions: {}
    };

    function readStorage() {
        let storageStr = localStorage.getItem('widgetStorage_' + localStorage.getItem('Meteor.userId') + '_' + options.channel);
        if(storageStr) {
            widgetStorage = JSON.parse(storageStr);
        } else {
            writeStorage();
        }
    }

    function writeStorage() {
        widgetStorage.tokens.userTokens = widgetHeaderTagsTemplateData && widgetHeaderTagsTemplateData.userTokens || [];
        widgetStorage.tokens.include = widgetHeaderTagsTemplateData && widgetHeaderTagsTemplateData.include || [];
        widgetStorage.tokens.exclude = widgetHeaderTagsTemplateData && widgetHeaderTagsTemplateData.exclude || [];
        if(!widgetStorage.widgetOptions) widgetStorage.widgetOptions = {};
        widgets.forEach((w) => {
            if(w && w.params.elem && w.queryCreator == "queryBuilder:conversationsearch") {
                widgetStorage.widgetOptions[w.params.query.creator] = {filters: {}};
                w.params.templateData.filters.forEach(fq => {
                    let enabled = widgetStorage.widgetOptions[w.params.query.creator] && widgetStorage.widgetOptions[w.params.query.creator].filters[fq.filter];
                    if(typeof enabled == "undefined") enabled = fq.enabled;
                    widgetStorage.widgetOptions[w.params.query.creator].filters[fq.filter] = enabled;
                });
            }
        });
        localStorage.setItem('widgetStorage_' + localStorage.getItem('Meteor.userId') + '_' + options.channel, JSON.stringify(widgetStorage));
    }

    readStorage();

    const widgetHeaderTagsTemplateStr = `
        {^{if widgets.length > 0}}
        <span>${Utils.localize({code: 'widget.tags.label'})}</span>
        <ul>
            <li class="add"><i class="icon-plus"></i></li>
            {^{for userTokens}}
            <li class="user-tag"><div class="title">{{:}}</div><div class="actions"><a class="action-remove" title="${Utils.localize({code: 'widget.latch.query.remove'})}"><i class="icon-trash"></i></a></div></li>
            {{/for}}
            {^{for tokens}}
            <li class="system-tag" data-link="class{merge: pinned toggle='pinned'}">
                <div class="title">{{:value}}</div>
                <div class="actions">
                    <a class="action-pin" data-link="title{: pinned?'${Utils.localize({code: 'widget.latch.query.unpin'})}':'${Utils.localize({code: 'widget.latch.query.pin'})}' }"><i class="icon-pin"></i></a>
                    {^{if !pinned}}<a class="action-remove" title="${Utils.localize({code: 'widget.latch.query.remove'})}"><i class="icon-trash"></i></a>{{/if}}
                </div>
            </li>
            {{/for}}
        </ul>
        <div class="tag-actions">
            {^{if userTokens.length + tokens.length > 0}}<span class="remove-all">${Utils.localize({code: 'widget.latch.query.remove.all'})}</span>{{/if}}
        </div>
        {{/if}}
    `;
    /*
        {^{if exclude.length}}<span>{^{:exclude.length}} ${Utils.localize({code: 'widget.latch.query.excluded'})}</span><span class="reset-exclude" title="${Utils.localize({code: 'widget.latch.query.reset'})}"><i class="icon-ccw"></i></span>{{/if}}
    */

    /*
        <div id="tabContainer">
            <span class="nav-item current">{^{if widgets.length}}{^{:widgets[selectedWidget].params.query.displayTitle}} ({^{:widgets[selectedWidget].params.templateData.results.length || 0}}){{/if}}</span>
            <span class="nav-item more">${Utils.localize({code: 'smarti.sources'})}</span>
        </div>
    */
    const widgetHeaderTabsTemplateStr = `
        <ul class="sources">
            {^{for widgets}}
            <li>{^{:params.query.displayTitle}} ({^{:params.templateData.total}})</li>
            {{/for}}
        </ul>
    `;
    const widgetHeaderInnerTabSearchTemplateStr = `
        <input type="search" placeholder="" data-link="placeholder{: 'Suchen in &#34;' + containerTitle + '&#34;' }">
        <div id="innerTabSearchSubmit">
            <div class="submit-icon"></div>
        </div>
    `;
    const widgetHeaderInnerTabFilterTemplateStr = `
        <span>${Utils.localize({code: 'widget.filters.label'})}</span>
        <ul>
            {^{for filters}}
            <li class="filter" data-link="class{merge: enabled toggle='enabled'}"><div class="title">{{:label}}</div></li>
            {{/for}}
        </ul>
    `;
    const widgetFooterPostButtonTemplateStr = `
        <span><i class="icon-paper-plane"></i> {^{:title}}</span>
    `;
    const widgetConversationTemplateStr = `
        {^{for results}}
            {^{if ~root.isRCSearch}}
                <div class="rc-search-result">
                    <div class="middle">
                        <div class="datetime">
                            {^{rcdt:time}} <a class="jump-link" data-link="href{:messageLink}" title="${Utils.localize({code: 'rc.search.jump'})}"><i class="icon-link-ext"></i></a> <span class="copy" title="${Utils.localize({code: 'rc.search.copy'})}"><i class="icon-docs"></i></span>
                        </div>
                        <div class="user-avatar"><img data-link="src{:'/avatar/' + username}"></div>
                        <div class="title">{^{: '@' + username + ' in #' + roomName}}</div>
                        <div class="text"><p>{^{:content}}</p></div>
                        <!--<div class="postAction">${Utils.localize({code: 'widget.post-message'})}</div>-->
                        <!--<div class="selectMessage"></div>-->
                    </div>
                </div>
            {{else}}
                <div class="conversation">
                    {^{if messagesBefore && messagesBefore.length}}
                        <div class="beforeContextContainer">
                            {^{for messagesBefore}}
                                <div class="convMessage">
                                    <div class="middle">
                                        <div class="datetime">
                                        {{tls:time}}
                                        </div>
                                        <div class="title"></div>
                                        <div class="text"><p>{{:~hl(~he(content || ''), true)}}</p></div>
                                        <div class="postAction">${Utils.localize({code: 'widget.post-message'})}</div>
                                        <div class="selectMessage"></div>
                                    </div>
                                </div>
                            {{/for}}
                        </div>
                    {{/if}}
                    <div class="convMessage" data-link="class{merge: messagesCnt toggle='parent'}">
                        <div class="middle">
                            <div class="datetime">
                                {{tls:time}}
                                {^{if isTopRated}}<span class="topRated">Top</span>{{/if}}
                                {^{if messagesCnt}}<span class="context">${Utils.localize({code: 'widget.show_details'})}</span>{{/if}}
                            </div>
                            <div class="title"></div>
                            <div class="text"><p>{{:~hl(~he(content || ''), true)}}</p></div>
                            <div class="postAction">${Utils.localize({code: 'widget.post-message'})}</div>
                            <div class="selectMessage"></div>
                        </div>
                    </div>
                    {^{if messagesAfter && messagesAfter.length}}
                        <div class="afterContextContainer">
                            {^{for messagesAfter}}
                                <div class="convMessage">
                                    <div class="middle">
                                        <div class="datetime">
                                        {{tls:time}}
                                        </div>
                                        <div class="title"></div>
                                        <div class="text"><p>{{:~hl(~he(content || ''), true)}}</p></div>
                                        <div class="postAction">${Utils.localize({code: 'widget.post-message'})}</div>
                                        <div class="selectMessage"></div>
                                    </div>
                                </div>
                            {{/for}}
                        </div>
                    {{/if}}
                </div>
            {{/if}}
        {{else}}
            {^{if msg}}
                <div class="msg">{^{:msg}}</div>
            {{else}}
                {^{if !loading}}
                    {^{if noMessages}}
                        <div class="no-result">${Utils.localize({code: 'widget.conversation.no-results-no-msg'})}</div>
                    {{else}}
                        <div class="no-result">${Utils.localize({code: 'widget.conversation.no-results'})}</div>
                    {{/if}}
                {{/if}}
            {{/if}}
        {{/for}}
        {^{if loading}}
            <div class="loading-animation"><div class="bounce bounce1"></div><div class="bounce bounce2"></div><div class="bounce bounce3"></div></div>
        {{/if}}
    `;
    const widgetIrLatchTemplateStr = `
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
            {^{if !loading}}
                <div class="no-result">{^{if noTags}}${Utils.localize({code: 'widget.latch.query.no-tags'})}{{else}}${Utils.localize({code: 'widget.latch.query.no-results'})}{{/if}}</div>
            {{/if}}
        {{/for}}
        {^{if loading}}
            <div class="loading-animation"><div class="bounce bounce1"></div><div class="bounce bounce2"></div><div class="bounce bounce3"></div></div>
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
    let noChatCloseBtn = !$('#widgetFooter .help-request-actions').length;


    let tags = $('<div id="tags">').appendTo(widgetHeaderWrapper);
    let tabs = $('<nav id="tabs">').appendTo(widgetHeader);
    let innerTabSearch = $('<div id="innerTabSearch">').appendTo(widgetHeader);
    let innerTabFilter = $('<div id="innerTabFilter">').appendTo(widgetHeader);

    let widgetContent = $('<div class="widgetContent"><div class="loading-animation"><div class="bounce bounce1"></div><div class="bounce bounce2"></div><div class="bounce bounce3"></div></div></div>').appendTo(widgetBody);

    let footerPostButton = $('<button class="button button-block" id="postSelected">').prependTo(widgetFooter);

    widgetTitle.css('marginBottom', '10px');

    widgetMessage.empty();
    tabs.hide();
    innerTabSearch.hide();
    innerTabFilter.hide();

    let widgetHeaderTagsTemplate = $.templates(widgetHeaderTagsTemplateStr);
    let widgetHeaderTabsTemplate = $.templates(widgetHeaderTabsTemplateStr);
    let widgetHeaderInnerTabSearchTemplate = $.templates(widgetHeaderInnerTabSearchTemplateStr);
    let widgetHeaderInnerTabFilterTemplate = $.templates(widgetHeaderInnerTabFilterTemplateStr);
    let widgetFooterPostButtonTemplate = $.templates(widgetFooterPostButtonTemplateStr);
    let widgetConversationTemplate = $.templates(widgetConversationTemplateStr);
    let widgetIrLatchTemplate = $.templates(widgetIrLatchTemplateStr);

    let widgetHeaderTagsTemplateData = {
        tokens: [],
        userTokens: widgetStorage.tokens.userTokens,
        include: widgetStorage.tokens.include,
        exclude: widgetStorage.tokens.exclude,
        widgets
    };
    widgetHeaderTagsTemplate.link(tags, widgetHeaderTagsTemplateData);

    let widgetHeaderTabsTemplateData = {widgets: widgets, selectedWidget: 0};
    widgetHeaderTabsTemplate.link(tabs, widgetHeaderTabsTemplateData);

    let widgetHeaderInnerTabSearchTemplateData = {containerTitle: ""};
    widgetHeaderInnerTabSearchTemplate.link(innerTabSearch, widgetHeaderInnerTabSearchTemplateData);

    let widgetFooterPostButtonTemplateData = {title: Utils.localize({code:'widget.conversation.post-selected', args:[0]})};
    widgetFooterPostButtonTemplate.link(footerPostButton, widgetFooterPostButtonTemplateData);

    let innerTabSearchInput = innerTabSearch.find('input');
    let innerTabSearchSubmit = innerTabSearch.find('#innerTabSearchSubmit');

    function adjustFooter() {
        if(noChatCloseBtn) {
            widgetFooter.hide();
            footerPostButton.css('position', 'relative');
        } else {
            footerPostButton.css('position', 'absolute');
            footerPostButton.css('transform', 'translateY(200%)');
            widgetFooter.show();
        }
    }

    const MutationObserver = window.MutationObserver || window.WebKitMutationObserver;
    let footerObserver = new MutationObserver((mutationRecords) => {
        //log.debug("Footer Observer:", mutationRecords);
        noChatCloseBtn = !$('#widgetFooter .help-request-actions').length;
        adjustFooter();
    });
    footerObserver.observe(widgetFooter.get(0), {childList: true});

    adjustFooter();

    //Smarti

    let smarti = new Smarti({
      DDP: {
        endpoint: options.socketEndpoint
      },
      tracker: tracker,
      channel: options.channel
    });//TODO wait for connect?

    smarti.subscribe('smarti.data', (data) => {
        refreshWidgets(data);
    });

    smarti.login(
        initialize,
        drawLogin
    );

    //append lightbulb close (only one handler!)
    let tabOpenButton = $('.flex-tab-container .flex-tab-bar .icon-lightbulb').parent();

    tabOpenButton.unbind('click.closeTracker');

    tabOpenButton.bind('click.closeTracker', () => {
        if($('.external-search-content').is(":visible")) {
            tracker.trackEvent('sidebar.close');
        }
    });

    // widget interaction logic
    let selectionCount = 0;

    widgetBody.scroll((event) => {
        if (widgetBody.scrollTop() > 1) {
            // Hide title and filters only when the gained height is less than the overflowing height,
            // otherwise there will be no scrolling and the UI will reset right away!
            if(widgetBody.prop('scrollHeight') - widgetBody.innerHeight() > widgetTitle.height() + innerTabFilter.height() + 10) {
                widgetTitle.slideUp(200);
                if(innerTabFilter.hasClass('active')) innerTabFilter.slideUp(200);
            }
            widgetHeader.addClass('shadow');
        } else {
            widgetTitle.slideDown(200);
            if(innerTabFilter.hasClass('active')) innerTabFilter.slideDown(200);
            widgetHeader.removeClass('shadow');
        }

        if(Math.round(widgetBody.prop('scrollHeight')) == Math.round(widgetBody.innerHeight() + widgetBody.scrollTop())) {
            widgetFooter.removeClass('shadow');
        } else {
            widgetFooter.addClass('shadow');
        }
    });

    let sources = tabs.find('.sources');

    tabs.on('click', '.more', function() {
        if (sources.hasClass('open')) {
            sources.slideUp(100).removeClass('open');
            $(this).text(Utils.localize({code: 'smarti.sources'}));
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

                if(newWidget.params.templateData.filters && newWidget.params.templateData.filters.length) {
                    let widgetHeaderInnerTabFilterTemplateData = {filters: newWidget.params.templateData.filters};
                    widgetHeaderInnerTabFilterTemplate.link(innerTabFilter, widgetHeaderInnerTabFilterTemplateData);

                    innerTabFilter.addClass('active');
                    if(widgetBody.scrollTop() > 1) {
                        innerTabFilter.slideUp(100);
                    } else {
                        innerTabFilter.slideDown(100);
                    }
                } else {
                    innerTabFilter.slideUp(100);
                }
            } else {
                //innerTabSearch.addClass('active');
                if(widgetBody.scrollTop() > 1) {
                    innerTabSearch.slideUp(100);
                } else {
                    //innerTabSearch.slideDown(100);
                }

                innerTabFilter.removeClass('active');
                innerTabFilter.slideUp(100);
            }

            innerTabSearchInput.val("");
            searchTerms = [];
            //search(widgetHeaderTabsTemplateData.selectedWidget);

            //sources.slideUp(100).removeClass('open');
            //tabs.find('.more').text(Utils.localize({code: 'smarti.sources'}));

            if(currentWidget) currentWidget.params.elem.find('.selected').removeClass('selected');
            selectionCount = 0;
            if(noChatCloseBtn) {
                widgetFooter.slideUp(200);
            } else {
                footerPostButton.css('transform', 'translateY(200%)');
            }

            if(newWidget.params.elem.height() <= widgetBody.innerHeight()) {
                widgetFooter.removeClass("shadow");
            } else {
                if(Math.round(widgetBody.prop('scrollHeight')) == Math.round(widgetBody.innerHeight() + widgetBody.scrollTop())) {
                    widgetFooter.removeClass("shadow");
                } else {
                    widgetFooter.addClass("shadow");
                }
            }

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

        /*
        if (
            parent.hasClass('parent') &&
            parent.hasClass('selected') &&
            !$(this).closest('.conversation').children('.afterContextContainer').is(':visible')
        ) {
            toggleConversation($(this).closest('.conversation'));
        }
        */

        if(selectionCount === 0) {
            if(noChatCloseBtn) {
                widgetFooter.slideUp(200);
            } else {
                footerPostButton.css('transform', 'translateY(200%)');
            }
        } else {
            $.observable(widgetFooterPostButtonTemplateData).setProperty("title", Utils.localize({code:'widget.conversation.post-selected' + (selectionCount == 1 ? '' : '-all'), args:[selectionCount]}));
            if(noChatCloseBtn) {
                widgetFooter.slideDown(200);
            } else {
                footerPostButton.css('transform', 'translateY(0)');
            }
        }
    });

    widgetBody.on('click', '.convMessage.parent .context', function() {
        const $conversation = $(this).closest('.conversation');
        toggleConversation($conversation);
    });

    function toggleConversation($conversation) {
        function afterCollapse() {
            $conversation.removeClass('expanded');
            $conversation.find('.context').text(Utils.localize({code: 'widget.show_details'}));
        }
        function afterExpand() {
            $conversation.find('.context').text(Utils.localize({code: 'widget.hide_details'}));
        }
        if($conversation.hasClass('expanded')) {
            $conversation.children('.beforeContextContainer').toggle(200, afterCollapse);
            $conversation.children('.afterContextContainer').toggle(200, afterCollapse);
        } else {
            $conversation.addClass('expanded');
            $conversation.children('.beforeContextContainer').toggle(200, afterExpand);
            $conversation.children('.afterContextContainer').toggle(200, afterExpand);
        }
        tracker.trackEvent("conversation.part.toggle");
    }

    function postItems(items) {
        function createTextMessage(text, conv) {
            if(conv.parent.templateType === "related.conversation") {
                text = text + '\n>' + conv.parent.content.replace(/\r\n/g, "\n").replace(/\n/g, " ");
            } else {
                text = text + '\n>' + (conv.parent.link ? '[' + conv.parent.title + '](' + conv.parent.link + ')' : conv.parent.title) + (conv.parent.description ? ': ' + conv.parent.description : '');
            }
            $.each(conv.selectedChildIndicesBefore, (i, childIdx) => {
                text += createTextMessage('', {parent : conv.parent.messagesBefore[childIdx]});
            });
            $.each(conv.selectedChildIndicesAfter, (i, childIdx) => {
                text += createTextMessage('', {parent : conv.parent.messagesAfter[childIdx]});
            });
            return text;
        }
        function buildAttachments(conv) {
            let attachment;
            if(conv.parent.templateType === "related.conversation") {
                attachment = {
                    text: conv.parent.content,
                    attachments: [],
                    bot: 'assistify',
                    ts: conv.parent.time
                };
                $.each(conv.selectedChildIndicesBefore, (i, childIdx) => {
                    attachment.attachments.push(buildAttachments({parent: conv.parent.messagesBefore[childIdx]}));
                });
                $.each(conv.selectedChildIndicesAfter, (i, childIdx) => {
                    attachment.attachments.push(buildAttachments({parent: conv.parent.messagesAfter[childIdx]}));
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
        if(items && items.length) {
            let text;
            if(items[0].parent.templateType === "related.conversation") {
                if(items.length > 1) {
                    text = Utils.localize({code:'widget.conversation.answer.title_msgs'});
                } else {
                    text = Utils.localize({code:'widget.conversation.answer.title' + (items[0].selectedChildIndicesBefore.length || items[0].selectedChildIndicesAfter.length ? '' : '_msg')});
                }
            } else {
                text = Utils.localize({code:"widget.latch.answer.title", args:[widgets[widgetHeaderTabsTemplateData.selectedWidget].params.query.displayTitle]});
            }

            if(options.postings && (options.postings.type === 'suggestText' || options.postings.type === 'postText')) {
                items.forEach(conv => {
                    text = createTextMessage(text, conv);
                });
                if(options.postings.type === 'suggestText') {
                    messageInputField.post(text);
                } else {
                    smarti.post(text, [], null, showError);
                }
            } else {
                let attachments = [];
                items.forEach(conv => {
                    attachments.push(buildAttachments(conv));
                });
                smarti.post(text, attachments, null, showError);
            }
        }
    }

    widgetBody.on('click', '.rc-search-result .copy', function() {
        let parent = $(this).parent().parent().parent();
        let parentResultIndex = $.view(parent).getIndex();
        let text = parent.text().trim().split(/\n/g).map(l => l.trim()).join("\r\n") + "\r\n";
        clipboard.copy(text);
        tracker.trackEvent("rc.search.result.copy", parentResultIndex);
    });

    widgetBody.on('click', '.rc-search-result .jump-link', function() {
        let parent = $(this).parent().parent().parent();
        let parentResultIndex = $.view(parent).getIndex();
        tracker.trackEvent("rc.search.result.jump", parentResultIndex);
    });

    footerPostButton.click(() => {
        let currentWidget = widgets[widgetHeaderTabsTemplateData.selectedWidget];
        if(currentWidget && currentWidget.params.elem) {
            let selectedItems = [];
            currentWidget.params.elem.find('.conversation>.convMessage, .irl-result').each((idx, item) => {
                let parentMessageData = $.view(item).data;
                let parentIsSelected = $(item).hasClass('selected');
                let conv = {parent: parentMessageData, selectedChildIndicesBefore: [], selectedChildIndicesAfter: []};
                if(parentIsSelected) selectedItems.push(conv);

                let beforeContextContainer = $(item).closest('.conversation').children('.beforeContextContainer');
                if(beforeContextContainer.length) {
                    beforeContextContainer.find('.convMessage').each((idx, item) => {
                        let childData = $.view(item).data;
                        let childIndex = $.view(item).index;
                        if($(item).hasClass('selected')) {
                            if(parentIsSelected) {
                                conv.selectedChildIndicesBefore.push(childIndex);
                            } else {
                                selectedItems.push({parent: childData, selectedChildIndicesBefore: [], selectedChildIndicesAfter: []});
                            }
                        }
                    });
                }

                let afterContextContainer = $(item).closest('.conversation').children('.afterContextContainer');
                if(afterContextContainer.length) {
                    afterContextContainer.find('.convMessage').each((idx, item) => {
                        let childData = $.view(item).data;
                        let childIndex = $.view(item).index;
                        if($(item).hasClass('selected')) {
                            if(parentIsSelected) {
                                conv.selectedChildIndicesAfter.push(childIndex);
                            } else {
                                selectedItems.push({parent: childData, selectedChildIndicesBefore: [], selectedChildIndicesAfter: []});
                            }
                        }
                    });
                }

            });
            log.debug("selected items:", selectedItems);
            postItems(selectedItems);
            tracker.trackEvent("conversation.post");
        }
    });

    widgetBody.on('click', '.smarti-widget .postAction', function() {
        let selectedItems = [];
        let parent = $(this).parent().parent();
        let parentMessageData = $.view(parent).data;
        let conv = {
            parent: parentMessageData,
            selectedChildIndicesBefore: [],
            selectedChildIndicesAfter: []
            //selectedChildIndicesBefore: parentMessageData.messagesBefore && parentMessageData.messagesBefore.length ? Array.apply(null, {length: parentMessageData.messagesBefore.length}).map(Number.call, Number) : [],
            //selectedChildIndicesAfter: parentMessageData.messagesAfter && parentMessageData.messagesAfter.length ? Array.apply(null, {length: parentMessageData.messagesAfter.length}).map(Number.call, Number) : []
        };
        selectedItems.push(conv);
        log.debug("selected items:", selectedItems);
        postItems(selectedItems);
        tracker.trackEvent("conversation.part.post", $.view(parent).getIndex());
    });

    let searchTimeout = null;
    let searchTerms = [];
    function search(widgetIdx) {
        if(searchTimeout) clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            searchTimeout = null;

            /*
            searchTerms = innerTabSearchInput.val();
            searchTerms = searchTerms.trim().toLowerCase().replace(/\s+/g, ' ');
            searchTerms = searchTerms.length ? searchTerms.split(' ') : [];
            */

            widgets.forEach((w, idx) => {
                if(w && w.params.elem && w.queryCreator != "queryBuilder:conversationmlt" && (typeof widgetIdx != "number" || idx == widgetIdx)) {
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

    tags.on('click', '.action-remove', function() {
        let $li = $(this).closest('li');
        let tokenIdx = $.view($li).index;
        let tokenData = $.view($li).data;
        if(typeof tokenData === "string") {
            $.observable(widgetHeaderTagsTemplateData.userTokens).remove(tokenIdx);
        } else {
            $.observable(widgetHeaderTagsTemplateData.exclude).insert(tokenData.value.trim().toLowerCase());
            $.observable(widgetHeaderTagsTemplateData.tokens).remove(tokenIdx);
            smarti.refresh();
        }

        tracker.trackEvent('tag.remove', tokenIdx);
    });

    tags.on('click', '.action-pin', function() {
        let $li = $(this).closest('li');
        let tokenIdx = $.view($li).index;
        let tokenData = $.view($li).data;

        if($li.hasClass('pinned')) {
            $.observable(tokenData).setProperty("pinned", false);
            let includeIdx = -1;
            widgetHeaderTagsTemplateData.include.some((t, idx) => {
                if(t.value == tokenData.value) {
                    includeIdx = idx;
                    return true;
                }
                return false;
            });
            if(includeIdx > -1) $.observable(widgetHeaderTagsTemplateData.include).remove(includeIdx);
            $(this).attr('title', Utils.localize({code: 'widget.latch.query.pin'}));
            $li.removeClass('pinned');
            tracker.trackEvent('tag.unpin', tokenIdx);
        } else {
            $.observable(tokenData).setProperty("pinned", true);
            $.observable(widgetHeaderTagsTemplateData.include).insert(tokenData);
            $(this).attr('title', Utils.localize({code: 'widget.latch.query.unpin'}));
            $li.addClass('pinned');
            tracker.trackEvent('tag.pin', tokenIdx);
        }
    });

    tags.on('click', '.remove-all', function() {
        $.observable(widgetHeaderTagsTemplateData.userTokens).refresh([]);
        $.observable(widgetHeaderTagsTemplateData.include).refresh([]);
        widgetHeaderTagsTemplateData.tokens.forEach(t => {
            $.observable(widgetHeaderTagsTemplateData.exclude).insert(t.value.trim().toLowerCase());
        });
        $.observable(widgetHeaderTagsTemplateData.tokens).refresh([]);
        smarti.refresh();
        tracker.trackEvent('tag.remove-all');
    });

    tags.on('click', '.reset-exclude', function() {
        $.observable(widgetHeaderTagsTemplateData.exclude).refresh([]);
        tracker.trackEvent('tag.reset-exclude');
        smarti.refresh();
    });

    tags.on('click', 'li.add', function() {
        if(!$(this).hasClass('active')) {
            $(this).addClass('active').html(`<input type="text" id="newTagInput" placeholder="${Utils.localize({code: 'smarti.new-search-term'})}">`);
            tags.find('#newTagInput').focus();
        }
    });

    $(tags).keydown((e) => {
        if (tags.find('li.add').hasClass('active')) {
            if(e.which == 13) {
                const newTag = tags.find('#newTagInput').val().trim();
                if(newTag != "") {
                    if(widgetHeaderTagsTemplateData.tokens.map(t => t.value.toLowerCase()).concat(widgetHeaderTagsTemplateData.userTokens).indexOf(newTag.toLowerCase()) == -1) {
                        $.observable(widgetHeaderTagsTemplateData.userTokens).insert(newTag);
                    }
                    tags.find('li.add').html('<i class="icon-plus"></i>').removeClass('active');
                }
                tracker.trackEvent('tag.add', 0);
            }
            if(e.which == 13 || e.which == 27) {
                tags.find('li.add').html('<i class="icon-plus"></i>').removeClass('active');
                event.preventDefault();
                event.stopPropagation();
            }
        }
    });

    innerTabFilter.on('click', '.filter', function() {
        let filterData = $.view($(this)).data;
        if($(this).hasClass('enabled')) {
            $.observable(filterData).setProperty("enabled", false);
        } else {
            $.observable(filterData).setProperty("enabled", true);
        }
    });

    let scrollTimeout = null;
    widgetBody.scroll(function() {
        if(scrollTimeout) clearTimeout(scrollTimeout);
        scrollTimeout = setTimeout(() => {
            scrollTimeout = null;
            if(Math.round(widgetBody.prop('scrollHeight')) == Math.round(widgetBody.innerHeight() + widgetBody.scrollTop())) {
                let currentWidget = widgets[widgetHeaderTabsTemplateData.selectedWidget];
                if(currentWidget.queryCreator != "queryBuilder:conversationmlt") currentWidget.loadNextPage();
            }
        }, 500);
    });

    function onDataChange() {
        writeStorage();
        search();
    }

    $.observable(widgetHeaderTagsTemplateData.tokens).observeAll(onDataChange);
    $.observable(widgetHeaderTagsTemplateData.userTokens).observeAll(onDataChange);
    $.observable(widgetHeaderTagsTemplateData.exclude).observeAll(onDataChange);
    $.observable(widgetHeaderTagsTemplateData.include).observeAll(function() {
        writeStorage();
    });

    $.views.helpers({
        // helper method to highlight text
        hl: (text, noSearchTerms) => {
            if(!text) return text;
            let terms = widgetHeaderTagsTemplateData.tokens.map(t => t.value).concat(widgetHeaderTagsTemplateData.userTokens);
            if(!noSearchTerms) terms = terms.concat(searchTerms);
            terms = [...new Set(terms)]; // unique terms
            terms.forEach(t => {
                text = text.replace(new RegExp(`(${escapeRegExp(t)})`, 'ig'), '<mark>$1</mark>');
            });
            return text;
        },
        // helper method for html encoding and new line conversion for jsrender/views
        he: (text) => {
            return $('<div/>').text(text).html().replace(/\n/g, '<br />');
        }
    });

    return {};
}

// custom timestamp to local string converter for jsrender/views
$.views.converters("tls", (val) => {
    return moment(val).format(Utils.localize({code: 'smarti.date-format'}));
});

// custom converter for RC message date and time display
$.views.converters("rcdt", (val) => {
    return moment(val).format(RocketChat.settings.get('Message_DateFormat') + ' - ' + RocketChat.settings.get('Message_TimeFormat'));
});

function escapeRegExp(str) {
    return str.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&");
}

function equalArrays(a, b) {
    return ld_lang.isEqual([...a].sort(), [...b].sort());
}

function getSolrQuery(queryArray) {
    return queryArray.map(q => '"' + q.replace(/[\\"]/g) + '"', '').join(' ');
}

function getRCMessageLink(rid, mid) {
    const subscription = RocketChat.models.Subscriptions.findOne({rid});
    const roomLink = RocketChat.roomTypes.getRouteLink(subscription.t, subscription);
    return roomLink ? `${roomLink}?msg=${mid}` : '';
}

const getProp = (p, o) => p.reduce((xs, x) => (xs && xs[x]) ? xs[x] : null, o);

window.SmartiWidget = SmartiWidget;
