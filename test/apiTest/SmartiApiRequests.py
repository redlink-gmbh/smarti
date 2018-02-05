import exrex
import logging
import names
import requests


# Authentification


class Authentification:

  def __init__(self, *args, **kwargs):
    if len(args) == 1:
      self.initWithToken(args[0])
    elif len(args) == 2:
      self.initWithUser(args[0], args[1])
    else:
      res = 'Invalid number of arguments!\n'
      res += 'args given:\t'+str(len(args))+'\n'
      res += 'expected: 1 (token) or 2 (username, password)'
      raise Exception(res)

  def __str__(self):
    if self.basicAuth():
      return 'Authentification with - Username: '+self.username+'\tPassword: '+self.password
    else:
      return 'Authentification with - Token: '+self.token

  def initWithUser(self, username, password):
    self.username = username
    self.password = password
    self.token = ''

  def initWithToken(self, token):
    self.token = token

  def basicAuth(self):
    if self.token == '':
      return True
    else:
      return False


# Requests

# Client

class SmartiRequests:

  OK_SUCCESS = 200
  CREATED = 201
  ACCEPTED = 202
  OK_NO_CONTENT = 204
  UPDATE_SUCCESS = 200

  PERM_ERROR = 403
  NOT_FOUND_ERROR = 404
  ALREADY_EXISTS_ERROR = 409
  INVALID_DATA_ERROR = 400

  def __init__(self, url, loglevel):
    self.url = url

    if loglevel.lower() == 'info':
      logging.basicConfig(level=logging.INFO)
    elif loglevel.lower() == 'debug':
      logging.basicConfig(level=logging.DEBUG)
    elif loglevel.lower() == 'warn':
      logging.basicConfig(level=logging.WARNING)
    else:
      logging.basicConfig(level=logging.ERROR)
    self.logger = logging.getLogger(__name__)

  def getClient(self, auth):
    if auth.basicAuth():
      res = requests.get(self.url+'client',
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password)
                         )
      return res
    else:
      res = requests.get(self.url+'client',
                         headers={'X-Auth-Token': auth.token}
                         )
      return res

  def postClient(self, auth, client):
    if auth.basicAuth():
      res = requests.post(self.url+'client',
                          auth=requests.auth.HTTPBasicAuth(
                              auth.username, auth.password),
                          json=client
                          )
      return res
    else:
      res = requests.post(self.url+'client',
                          headers={'X-Auth-Token': auth.token},
                          json=client
                          )
      return res

  def delClient(self, auth, clientid):
    if auth.basicAuth():
      res = requests.delete(self.url+'client/'+clientid,
                            auth=requests.auth.HTTPBasicAuth(
                                auth.username, auth.password)
                            )
      return res
    else:
      res = requests.delete(self.url+'client/'+clientid,
                            headers={'X-Auth-Token': auth.token}
                            )
      return res

  def getClientSingle(self, auth, clientid):
    if auth.basicAuth():
      res = requests.get(self.url+'client/'+clientid,
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password)
                         )
      return res
    else:
      res = requests.get(self.url+'client/'+clientid,
                         headers={'X-Auth-Token': auth.token}
                         )
      return res

  def getClientConfig(self, auth, clientid):
    if auth.basicAuth():
      res = requests.get(self.url+'client/'+clientid+'/config',
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password)
                         )
      return res
    else:
      res = requests.get(self.url+'client/'+clientid+'/config',
                         headers={'X-Auth-Token': auth.token}
                         )
      return res

  def postClientConfig(self, auth, clientid, config):
    if auth.basicAuth():
      res = requests.post(self.url+'client/'+clientid+'/config',
                          auth=requests.auth.HTTPBasicAuth(
                              auth.username, auth.password),
                          json=config
                          )
      return res
    else:
      res = requests.post(self.url+'client/'+clientid+'/config',
                          headers={'X-Auth-Token': auth.token},
                          json=config
                          )
      return res

  def getClientToken(self, auth, clientid):
    if auth.basicAuth():
      res = requests.get(self.url+'client/'+clientid+'/token',
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password)
                         )
      return res
    else:
      res = requests.get(self.url+'client/'+clientid+'/token',
                         headers={'X-Auth-Token': auth.token}
                         )
      return res

  def postClientToken(self, auth, clientid, token):
    if auth.basicAuth():
      res = requests.post(self.url+'client/'+clientid+'/token',
                          auth=requests.auth.HTTPBasicAuth(
                              auth.username, auth.password),
                          json=token
                          )
      return res
    else:
      res = requests.post(self.url+'client/'+clientid+'/token',
                          headers={'X-Auth-Token': auth.token},
                          json=token
                          )
      return res

  def delClientToken(self, auth, clientid, token):
    if auth.basicAuth():
      res = requests.delete(self.url+'client/'+clientid+'/token/'+token,
                            auth=requests.auth.HTTPBasicAuth(
                                auth.username, auth.password)
                            )
      return res
    else:
      res = requests.delete(self.url+'client/'+clientid+'/token/'+token,
                            headers={'X-Auth-Token': auth.token}
                            )
      return res

  def updateClientToken(self, auth, clientid, token, newToken):
    if auth.basicAuth():
      res = requests.put(self.url+'client/'+clientid+'/token/'+token,
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password),
                         json=newToken
                         )
      return res
    else:
      res = requests.put(self.url+'client/'+clientid+'/token/'+token,
                         headers={'X-Auth-Token': auth.token},
                         json=newToken
                         )
      return res

  def getClientUser(self, auth, clientid):
    if auth.basicAuth():
      res = requests.get(self.url+'client/'+clientid+'/user',
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password)
                         )
      return res
    else:
      res = requests.get(self.url+'client/'+clientid+'/user',
                         headers={'X-Auth-Token': auth.token}
                         )
      return res

  def postClientUser(self, auth, clientid, user):
    if auth.basicAuth():
      res = requests.post(self.url+'client/'+clientid+'/user',
                          auth=requests.auth.HTTPBasicAuth(
                              auth.username, auth.password),
                          json=user
                          )
      return res
    else:
      res = requests.post(self.url+'client/'+clientid+'/user',
                          headers={'X-Auth-Token': auth.token},
                          json=user
                          )
      return res

  def delClientUser(self, auth, clientid, username):
    if auth.basicAuth():
      res = requests.delete(self.url+'client/'+clientid+'/user/'+username,
                            auth=requests.auth.HTTPBasicAuth(
                                auth.username, auth.password)
                            )
      return res
    else:
      res = requests.delete(self.url+'client/'+clientid+'/user/'+username,
                            headers={'X-Auth-Token': auth.token}
                            )
      return res

  def updateClientUser(self, auth, clientid, username):
    if auth.basicAuth():
      res = requests.put(self.url+'client/'+clientid+'/user/'+username,
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password)
                         )
      return res
    else:
      res = requests.put(self.url+'client/'+clientid+'/user/'+username,
                         headers={'X-Auth-Token': auth.token}
                         )
      return res

  # Conversation

  def getConversation(self, auth, queries):
    if auth.basicAuth():
      res = requests.get(self.url+'conversation',
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password),
                         params=queries
                         )
      return res
    else:
      res = requests.get(self.url+'conversation',
                         headers={'X-Auth-Token': auth.token},
                         params=queries
                         )
      return res

  def postConversation(self, auth, conversation, queries):
    if auth.basicAuth():
      res = requests.post(self.url+'conversation',
                          auth=requests.auth.HTTPBasicAuth(
                              auth.username, auth.password),
                          json=conversation,
                          params=queries
                          )
      return res
    else:
      res = requests.post(self.url+'conversation',
                          headers={'X-Auth-Token': auth.token},
                          json=conversation,
                          params=queries
                          )
      return res

  def getConversationSearch(self, auth, queries):
    if auth.basicAuth():
      res = requests.get(self.url+'conversation/search',
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password),
                         params=queries
                         )
      return res
    else:
      res = requests.get(self.url+'conversation/search',
                         headers={'X-Auth-Token': auth.token},
                         params=queries
                         )
      return res

  def delConversation(self, auth, conversationid):
    if auth.basicAuth():
      res = requests.delete(self.url+'conversation/'+conversationid,
                            auth=requests.auth.HTTPBasicAuth(
                                auth.username, auth.password)
                            )
      return res
    else:
      res = requests.delete(self.url+'conversation/'+conversationid,
                            headers={'X-Auth-Token': auth.token}
                            )
      return res

  def getConversationSingle(self, auth, conversationid, queries):
    if auth.basicAuth():
      res = requests.get(self.url+'conversation/'+conversationid,
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password),
                         params=queries
                         )
      return res
    else:
      res = requests.get(self.url+'conversation/'+conversationid,
                         headers={'X-Auth-Token': auth.token},
                         params=queries
                         )
      return res

  def getConversationAnalysis(self, auth, conversationid, queries):
    if auth.basicAuth():
      res = requests.get(self.url+'conversation/'+conversationid+'/analysis',
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password),
                         params=queries
                         )
      return res
    else:
      res = requests.get(self.url+'conversation/'+conversationid+'/analysis',
                         headers={'X-Auth-Token': auth.token},
                         params=queries
                         )
      return res

  def postConversationAnalaysis(self, auth, conversationid, analysis, queries):
    if auth.basicAuth():
      res = requests.post(self.url+'conversation/'+conversationid+'/analysis',
                          auth=requests.auth.HTTPBasicAuth(
                              auth.username, auth.password),
                          json=analysis,
                          params=queries
                          )
      return res
    else:
      res = requests.post(self.url+'conversation/'+conversationid+'/analysis',
                          headers={'X-Auth-Token': auth.token},
                          json=analysis,
                          params=queries
                          )
      return res

  def getConversationAnalysisTemplate(self, auth, conversationid, queries):
    if auth.basicAuth():
      res = requests.get(self.url+'conversation/'+conversationid+'/analysis/template',
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password),
                         params=queries
                         )
      return res
    else:
      res = requests.get(self.url+'conversation/'+conversationid+'/analysis/template',
                         headers={'X-Auth-Token': auth.token},
                         params=queries
                         )
      return res

  def getConversationAnalysisTemplateSingle(self, auth, conversationid, templateIdx, queries):
    if auth.basicAuth():
      res = requests.get(self.url+'conversation/'+conversationid+'/analysis/template/'+templateIdx,
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password),
                         params=queries
                         )
      return res
    else:
      res = requests.get(self.url+'conversation/'+conversationid+'/analysis/template/'+templateIdx,
                         headers={'X-Auth-Token': auth.token},
                         params=queries
                         )
      return res

  def getConversationAnalysisTemplateResult(self, auth, conversationid, templateIdx, creator, queries):
    if auth.basicAuth():
      res = requests.get(self.url+'conversation/'+conversationid+'/analysis/template/'+templateIdx+'/result/'+creator,
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password),
                         params=queries
                         )
      return res
    else:
      res = requests.get(self.url+'conversation/'+conversationid+'/analysis/template/'+templateIdx+'/result/'+creator,
                         headers={'X-Auth-Token': auth.token},
                         params=queries
                         )
      return res

  def postConversationAnalysisTemplateResult(self, auth, conversationid, templateIdx, creator, analysis, queries):
    if auth.basicAuth():
      res = requests.post(self.url+'conversation/'+conversationid+'/analysis/template/'+templateIdx+'/result/'+creator,
                          auth=requests.auth.HTTPBasicAuth(
                              auth.username, auth.password),
                          json=analysis,
                          params=queries
                          )
      return res
    else:
      res = requests.post(self.url+'conversation/'+conversationid+'/analysis/template/'+templateIdx+'/result/'+creator,
                          headers={'X-Auth-Token': auth.token},
                          json=analysis,
                          params=queries
                          )
      return res

  def getConversationAnalysisToken(self, auth, conversationid, queries):
    if auth.basicAuth():
      res = requests.get(self.url+'conversation/'+conversationid+'/analysis/token',
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password),
                         params=queries
                         )
      return res
    else:
      res = requests.get(self.url+'conversation/'+conversationid+'/analysis/token',
                         headers={'X-Auth-Token': auth.token},
                         params=queries
                         )
      return res

  def getConversationMessage(self, auth, conversationid, queries):
    if auth.basicAuth():
      res = requests.get(self.url+'conversation/'+conversationid+'/message',
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password),
                         params=queries
                         )
      return res
    else:
      res = requests.get(self.url+'conversation/'+conversationid+'/message',
                         headers={'X-Auth-Token': auth.token},
                         params=queries
                         )
      return res

  def postConversationMessage(self, auth, conversationid, message, queries):
    if auth.basicAuth():
      res = requests.post(self.url+'conversation/'+conversationid+'/message',
                          auth=requests.auth.HTTPBasicAuth(
                              auth.username, auth.password),
                          json=message,
                          params=queries
                          )
      return res
    else:
      res = requests.post(self.url+'conversation/'+conversationid+'/message',
                          headers={'X-Auth-Token': auth.token},
                          json=message,
                          params=queries
                          )
      return res

  def delConversationMessage(self, auth, conversationid, messageid, queries):
    if auth.basicAuth():
      res = requests.delete(self.url+'conversation/'+conversationid+'/message/'+messageid,
                            auth=requests.auth.HTTPBasicAuth(
                                auth.username, auth.password),
                            params=queries
                            )
      return res
    else:
      res = requests.delete(self.url+'conversation/'+conversationid+'/message/'+messageid,
                            headers={'X-Auth-Token': auth.token},
                            params=queries
                            )
      return res

  def getConversationMessageSingle(self, auth, conversationid, messageid, queries):
    if auth.basicAuth():
      res = requests.get(self.url+'conversation/'+conversationid+'/message/'+messageid,
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password),
                         params=queries
                         )
      return res
    else:
      res = requests.get(self.url+'conversation/'+conversationid+'/message/'+messageid,
                         headers={'X-Auth-Token': auth.token},
                         params=queries
                         )
      return res

  def updateConversationMessage(self, auth, conversationid, messageid, message, queries):
    if auth.basicAuth():
      res = requests.put(self.url+'conversation/'+conversationid+'/message/'+messageid,
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password),
                         json=message,
                         params=queries
                         )
      return res
    else:
      res = requests.put(self.url+'conversation/'+conversationid+'/message/'+messageid,
                         headers={'X-Auth-Token': auth.token},
                         json=message,
                         params=queries
                         )
      return res

  def updateConversationMessageField(self, auth, conversationid, messageid, fieldname, field, queries):
    if auth.basicAuth():
      res = requests.put(self.url+'conversation/'+conversationid+'/message/'+messageid+'/'+fieldname,
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password),
                         json=field,
                         params=queries
                         )
      return res
    else:
      res = requests.put(self.url+'conversation/'+conversationid+'/message/'+messageid+'/'+fieldname,
                         headers={'X-Auth-Token': auth.token},
                         json=field,
                         params=queries
                         )
      return res

  def delConversationField(self, auth, conversationid, fieldname, queries):
    if auth.basicAuth():
      res = requests.delete(self.url+'conversation/'+conversationid+'/'+fieldname,
                            auth=requests.auth.HTTPBasicAuth(
                                auth.username, auth.password),
                            params=queries
                            )
      return res
    else:
      res = requests.delete(self.url+'conversation/'+conversationid+'/'+fieldname,
                            headers={'X-Auth-Token': auth.token},
                            params=queries
                            )
      return res

  def updateConversationField(self, auth, conversationid, fieldname, field, queries):
    if auth.basicAuth():
      res = requests.put(self.url+'conversation/'+conversationid+'/'+fieldname,
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password),
                         json=field,
                         params=queries
                         )
      return res
    else:
      res = requests.put(self.url+'conversation/'+conversationid+'/'+fieldname,
                         headers={'X-Auth-Token': auth.token},
                         json=field,
                         params=queries
                         )
      return res

  # User

  def getAuth(self):
    res = requests.get(self.url+'auth')
    return res

  def getAuthCheck(self, login):
    res = requests.get(self.url+'auth/check',
                       params={'login': login}
                       )
    return res

  def postAuthRecovery(self, login, data):
    res = requests.post(self.url+'auth/recover',
                        params={'user': login},
                        json=data
                        )
    return res

  def postAuthSignup(self, user):
    res = requests.post(self.url+'auth/signup',
                        json=user
                        )
    return res

  def getUser(self, auth, queries):
    if auth.basicAuth():
      res = requests.get(self.url+'user',
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password),
                         params=queries
                         )
      return res
    else:
      res = requests.get(self.url+'user',
                         headers={'X-Auth-Token': auth.token},
                         params=queries
                         )
      return res

  def postUser(self, auth, user):
    if auth.basicAuth():
      res = requests.post(self.url+'user',
                          auth=requests.auth.HTTPBasicAuth(
                              auth.username, auth.password),
                          json=user
                          )
      return res
    else:
      res = requests.post(self.url+'user',
                          headers={'X-Auth-Token': auth.token},
                          json=user
                          )
      return res

  def delUser(self, auth, login):
    if auth.basicAuth():
      res = requests.delete(self.url+'user/'+login,
                            auth=requests.auth.HTTPBasicAuth(
                                auth.username, auth.password)
                            )
      return res
    else:
      res = requests.delete(self.url+'user/'+login,
                            headers={'X-Auth-Token': auth.token}
                            )
      return res

  def getUserSingle(self, auth, login):
    if auth.basicAuth():
      res = requests.get(self.url+'user/'+login,
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password)
                         )
      return res
    else:
      res = requests.get(self.url+'user/'+login,
                         headers={'X-Auth-Token': auth.token}
                         )
      return res

  def updateUser(self, auth, login, user):
    if auth.basicAuth():
      res = requests.put(self.url+'user/'+login,
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password),
                         json=user
                         )
      return res
    else:
      res = requests.put(self.url+'user/'+login,
                         headers={'X-Auth-Token': auth.token},
                         json=user
                         )
      return res

  def updateUserPassword(self, auth, login, password):
    if auth.basicAuth():
      res = requests.put(self.url+'user/'+login+'/password',
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password),
                         json=password
                         )
      return res
    else:
      res = requests.put(self.url+'user/'+login+'/password',
                         headers={'X-Auth-Token': auth.token},
                         json=password
                         )
      return res

  def updateUserRoles(self, auth, login, roles):
    if auth.basicAuth():
      res = requests.put(self.url+'user/'+login+'/roles',
                         auth=requests.auth.HTTPBasicAuth(
                             auth.username, auth.password),
                         json=roles
                         )
      return res
    else:
      res = requests.put(self.url+'user/'+login+'/roles',
                         headers={'X-Auth-Token': auth.token},
                         json=roles
                         )
      return res

  # Helper functions

  def sendRequest(self, function, expectedResponse, *p):
    res = function(*p)
    if (res.status_code != expectedResponse):
      self.logger.warning('Request '+str(function).split(' ')[2].split(
          '.')[1]+' failed!\t\tErrorCode: '+str(res.status_code) + '\n' + res.text)
      raise Exception(res.url + '\nRequest failed: ' +
                      str(res.status_code) + '\texpected: ' + str(expectedResponse) + '\n' + res.text)
    else:
      self.logger.info('Request '+str(function).split(' ')
                       [2].split('.')[1]+' successful')
    return res

  def filterClientIds(self, clients):
    ids = []
    for c in clients:
      ids.append(c.get('id'))
    return ids

  def randomID24(self):
    return exrex.getone('(\d|[a-f]){24}')

  def randomToken(self):
    return exrex.getone('(\d|[a-f]){40}/(\d|[a-f]){8}')

  def randomMessage(self):
    text = 'Hallo, mein Name ist '+names.get_full_name()+'!\tDas ist eine Testnachricht!'
    message = {
        "content": text,
        "origin": "User",
        "private": False
    }
    return message

  def cleanup(self, auth):
    self.logger.info('Cleanup ...')
    cs = self.filterClientIds(self.sendRequest(
        self.getClient, SmartiRequests.OK_SUCCESS, auth).json())
    self.logger.info('Number of clients to clean: ' + str(len(cs)))
    for c in cs:
      self.sendRequest(self.delClient, SmartiRequests.OK_NO_CONTENT, auth, c)
    usernames = set()
    data = self.sendRequest(self.getUser, SmartiRequests.OK_SUCCESS, auth, {})
    for i in data.json():
      username = i.get('login')
      if username != 'admin':
        usernames.add(username)
    self.logger.info('Number of Users to clean: ' + str(len(usernames)))
    for i in usernames:
      self.sendRequest(self.delUser, SmartiRequests.OK_NO_CONTENT, auth, i)

  # def prettyPrintJson(content):
  #     print(json.dumps(content, sort_keys=True, indent=2, separators=(',', ': ')))
