import requests
import json
import random
import logging
import argparse
import unittest
from loremipsum import generate_paragraph


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


class RCRequests:

  GET_SUCCESS = 200
  POST_SUCCESS = 201
  DELETE_SUCCESS = 204
  UPDATE_SUCCESS = 200

  PERM_ERROR = 403
  NOT_FOUND_ERROR = 404
  ALREADY_EXISTS_ERROR = 409
  INVALID_DATA_ERROR = 400
  tc = unittest.TestCase('__init__')

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

  def login(self, auth):
    if auth.basicAuth():
      res = requests.post(url+'login',
                          json={"username": auth.username,
                                "password": auth.password}
                          )
      return res
    else:
      logger.error('Can\'t login to RC without username and password')

  def logout(self, auth):
    if auth.basicAuth():
      res = requests.get(url+'logout',
                         headers={"X-Auth-Token": auth.password,
                                  "X-User-Id": auth.username}
                         )
      return res
    else:
      logger.error('Can\'t logout to RC without userID and Auth-Token')

  def createRequest(self, auth, support_area, seeker, providers):
    if auth.basicAuth():
      res = requests.post(url+'assistify.helpDiscussion',
                          headers={"X-Auth-Token": auth.password,
                                   "X-User-Id": auth.username},
                          json={"support_area": support_area,
                                "seeker": seeker, "providers": providers}
                          )
      return res
    else:
      logger.error(
          'Can\'t use RC Rest-API without userID and Auth-Token')

  def getUserInfo(self, auth, username):
    if auth.basicAuth():
      res = requests.get(url+'users.info',
                         headers={"X-Auth-Token": auth.password,
                                  "X-User-Id": auth.username},
                         params={'username': username}
                         )
      return res
    else:
      logger.error(
          'Can\'t use RC Rest-API without userID and Auth-Token')

  def sendMessage(self, auth, roomId, text):
    if auth.basicAuth():
      res = requests.post(url+'chat.postMessage',
                          headers={"X-Auth-Token": auth.password,
                                   "X-User-Id": auth.username},
                          json={"roomId": roomId, "text": text}
                          )
      return res
    else:
      logger.error(
          'Can\'t use RC Rest-API without userID and Auth-Token')

  def randomMessage(self):
    _, _, text = generate_paragraph()
    return str(text).strip()

  def sendRequest(self, function, expectedResponse, *p):
    res = function(*p)
    if (res.status_code != expectedResponse):
      self.logger.warning('Request '+str(function).split(' ')
                          [2].split('.')[1]+' failed!\t\tErrorCode: '+str(res.status_code))
    else:
      self.logger.info('Request '+str(function).split(' ')
                       [2].split('.')[1]+' successful')
    self.tc.assertEquals(res.status_code, expectedResponse)
    return res


parser = argparse.ArgumentParser(
    description='Create Rocket.Chat Request Dataset')
parser.add_argument('--url', default='http://localhost:3000/', dest='url',
                    help='Set Rocket.Chat rest-url (Defaul: http://localhost:3000/)')
parser.add_argument('--username', default='rocketchat.internal.admin.test', dest='username',
                    help='Set Rocket.Chat login username. User must be an admin (Defaul: rocketchat.internal.admin.test)')
parser.add_argument('--password', default='rocketchat.internal.admin.test', dest='password',
                    help='Set Rocket.Chat login password. (Defaul: rocketchat.internal.admin.test)')
parser.add_argument('--loglevel', default='error', dest='loglevel', choices=('info', 'warn', 'debug', 'error'),
                    help='Set log level. (Defaul: error)')
parser.add_argument('--numRequests', default=5, dest='numRequests', type=int,
                    help='Set number of requests. (Defaul: 5)')
parser.add_argument('--maxMessages', default=3, dest='maxMessages', type=int,
                    help='Set number of requests. (Defaul: 3)')
args = parser.parse_args()

admin_user = args.username
admin_password = args.password
admin = Authentification(admin_user, admin_password)
url = args.url

if not url.endswith('/'):
  url = url+'/'
url = url + 'api/v1/'
rc = RCRequests(url, args.loglevel)
numRooms = args.numRequests
maxMessages = args.maxMessages


def createRequestsWithMessages(numRooms, maxMessages):
  for r in range(numRooms):
    numMessages = random.randint(1, maxMessages)
    r = rc.sendRequest(rc.login, 200, admin)
    rcauth = Authentification(r.json().get('data').get(
        'userId'), r.json().get('data').get('authToken'))
    r = rc.sendRequest(rc.getUserInfo, 200, rcauth, rcauth.username)
    user = r.json().get('user')
    usermail = r.json().get('user').get('emails')[0].get('address')
    userid = r.json().get('user').get('_id')
    seeker = {"id": userid, "email": usermail}
    r = rc.sendRequest(rc.createRequest, 200, rcauth,
                       'test', seeker, [seeker])
    roomId = r.json().get('room').get('_id')
    roomName = r.json().get('room').get('name')
    identifier = random.randint(0, 1)
    if identifier:
      rc.sendRequest(rc.sendMessage, 200, rcauth,
                     roomId, 'Diese Nachricht soll gefunden werden in einer Suche oder in nahen Konversationen')
    for m in range(numMessages):
      rc.sendRequest(rc.sendMessage, 200, rcauth,
                     roomId, rc.randomMessage())
  rc.sendRequest(rc.logout, 200, rcauth)


createRequestsWithMessages(numRooms, maxMessages)
