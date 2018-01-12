import requests
import names
import unittest
import exrex
import json
import random
import sys

admin_user = 'admin'
admin_password = 'admin'
url = 'http://localhost:8080/'

if '--user' in str(sys.argv):
    admin_user = sys.argv[sys.argv.index('--user')+1]
if '--pwd' in str(sys.argv):
    admin_password = sys.argv[sys.argv.index('--pwd')+1]
if '--url' in str(sys.argv):
    url = sys.argv[sys.argv.index('--url')+1]
    if not url.endswith('/'):
        url = url+'/'

# Requests

## Client
def createClient(name, description, default=False):
    res = requests.post(url+'client', 
        auth=requests.auth.HTTPBasicAuth(admin_user, admin_password), 
        json={'defaultClient': default, 'description': description, 'name': name}
        )
    return res

def getClients(username, password):
    res = requests.get(url+'client', auth=requests.auth.HTTPBasicAuth(username, password))
    return res

def getClientsT(token):
    res = requests.get(url+'client', headers={'X-Auth-Token': token})
    return res

def getSingleClient(username, password, clientid):
    res = requests.get(url+'client/'+clientid, auth=requests.auth.HTTPBasicAuth(username, password))
    return res

def delClient(username, password, clientid):
    res = requests.delete(url+'client/'+clientid, auth=requests.auth.HTTPBasicAuth(username, password))
    return res

def getConfig(username, password, clientid):
    res = requests.get(url+'client/'+clientid+'/config', auth=requests.auth.HTTPBasicAuth(username, password))
    return res

def postConfig(username, password, clientid, config):
    res = requests.post(url+'client/'+clientid+'/config', 
        auth=requests.auth.HTTPBasicAuth(username, password), 
        json=config
        )
    return res

def getTokens(username, password, clientid):
    res = requests.get(url+'client/'+clientid+'/token', auth=requests.auth.HTTPBasicAuth(username, password))
    return res

def postToken(username, password, clientid):
    res = requests.post(url+'client/'+clientid+'/token', 
        auth=requests.auth.HTTPBasicAuth(username, password), 
        json={}
        )
    return res

def delToken(username, password, clientid, token):
    res = requests.delete(url+'client/'+clientid+'/token/'+token, auth=requests.auth.HTTPBasicAuth(username, password))
    return res

def getUsersForClient(username, password, clientid):
    res = requests.get(url+'client/'+clientid+'/user', auth=requests.auth.HTTPBasicAuth(username, password))
    return res

def createUserForClient(username, password, clientid, user):
    res = requests.post(url+'client/'+clientid+'/user', 
        auth=requests.auth.HTTPBasicAuth(username, password), 
        json=user
        )
    return res

def assignUserToClient(username, password, clientid, userid):
    res = requests.put(url+'client/'+clientid+'/user/'+userid, 
        auth=requests.auth.HTTPBasicAuth(username, password), 
        )
    return res

def unassigneUserFromClient(username, password, clientid, userid):
    res = requests.delete(url+'client/'+clientid+'/user/'+userid, 
        auth=requests.auth.HTTPBasicAuth(username, password), 
        )
    return res

def delUser(username, password, userid):
    res = requests.delete(url+'user/'+userid, 
        auth=requests.auth.HTTPBasicAuth(username, password), 
        )
    return res

## Conversations

def postConversation(username, password, clientname, conversation):
    res = requests.post(url+'rocket/'+clientname, 
        auth=requests.auth.HTTPBasicAuth(username, password), 
        json=conversation
        )
    return res

def getConversationID(username, password, clientname, channelid):
    res = requests.get(url+'rocket/'+clientname+'/'+channelid+'/conversationid', auth=requests.auth.HTTPBasicAuth(username, password))
    return res

def getConversation(username, password, conversationid):
    res = requests.get(url+'conversation/'+conversationid, auth=requests.auth.HTTPBasicAuth(username, password))
    return res

def publishConversation(username, password, conversationid):
    res = requests.post(url+'conversation/'+conversationid+'/publish', auth=requests.auth.HTTPBasicAuth(username, password))
    return res

def getConversationAnalysis(username, password, conversationid):
    res = requests.get(url+'conversation/'+conversationid+'/analysis', auth=requests.auth.HTTPBasicAuth(username, password))
    return res

def getConversationResults(username, password, conversationid, template, creator):
    res = requests.get(url+'conversation/'+conversationid+'/template/'+template+'/'+creator, auth=requests.auth.HTTPBasicAuth(username, password))
    return res

## User

def setUserPassword(username, password, login, pw):
    res = requests.put(url+'user/'+login+'/password', auth=requests.auth.HTTPBasicAuth(username, password), json={"password": pw})
    return res

# Helper

def filterClientIds(clients):
    ids = []
    for c in clients:
        ids.append(c.get('id'))
    return ids

def randomID24():
    return exrex.getone('(\d|[a-f]){24}')

def randomToken():
    return exrex.getone('(\d|[a-f]){40}/(\d|[a-f]){8}')

def randomMessage():
    message = 'Hallo, my name is '+names.get_full_name()+'!\nThis is a Testmessage!'
    return message

def cleanup():
    cs = filterClientIds(getClients(admin_user, admin_password).json())
    for c in cs:
        delClient(admin_user, admin_password, c)

# Tests

class TestStringMethods(unittest.TestCase):

    @classmethod
    def tearDownClass(cls):
        cleanup()

    def test_clients(self):
        print('[Test Clients]')
        clientids = set()
        for i in range(10):
            c = createClient(names.get_last_name().lower(), '')
            self.assertEqual(c.status_code, 200)
            clientids.add(str(c.json().get('id')))
        self.assertEqual(len(clientids), 10)
        print('\tCreate clients: OK')
        cs = getClients(admin_user, admin_password)
        self.assertEqual(cs.status_code, 200)
        lc = filterClientIds(cs.json())
        for c in clientids:
            self.assertTrue(c in lc)
        print('\tGet created clients: OK')
        cleanup()
        
        
    def test_tokens(self):
        print('[Test Tokens]')
        c = createClient(names.get_last_name().lower(), '')
        self.assertEqual(c.status_code, 200)
        print('\tCreate clients: OK')
        clientid = str(c.json().get('id'))
        t = getTokens(admin_user, admin_password, clientid)
        self.assertEqual(t.status_code, 200)
        self.assertEqual(t.json(), [])
        print('\tGet tokens is empty: OK')
        t = postToken(admin_user, admin_password, clientid)
        self.assertEqual(t.status_code, 201)
        print('\tPost token success: OK')
        t = getTokens(admin_user, admin_password, clientid)
        self.assertEqual(t.status_code, 200)
        self.assertEqual(len(t.json()), 1)
        print('\tNew token exists: OK')
        tk = str(t.json()[0].get('id'))
        t = delToken(admin_user, admin_password, clientid, tk)
        self.assertEqual(t.status_code, 204)
        print('\tToken deleted: OK')
        cleanup()

    def test_token_usage(self):
        print('[Test Tokens Usage]')
        c = createClient(names.get_last_name().lower(), '')
        self.assertEqual(c.status_code, 200)
        print('\tCreate clients: OK')
        clientid = str(c.json().get('id'))
        t = postToken(admin_user, admin_password, clientid)
        self.assertEqual(t.status_code, 201)
        print('\tPost token success: OK')
        token = t.json().get('token')
        c = getClientsT(token)
        self.assertEqual(c.status_code, 200)
        self.assertEqual(len(c.json()), 1)
        print('\tGet clients via token: OK')
        cleanup()

    def test_token_invalid(self):
        print('[Test Tokens invalid]')
        c = createClient(names.get_last_name().lower(), '')
        self.assertEqual(c.status_code, 200)
        print('\tCreate clients: OK')
        clientid = str(c.json().get('id'))
        t = postToken(admin_user, admin_password, clientid)
        print('\tPost token success: OK')
        invalid_token = randomToken()
        c = getClientsT(invalid_token)
        self.assertEqual(c.status_code, 200)
        self.assertEqual(len(c.json()), 0)
        print('\tGet clients via invalid token fails: OK')
        cleanup()

    def test_users(self):
        print('[Test User]')
        c = createClient(names.get_last_name().lower(), '')
        self.assertEqual(c.status_code, 200)
        print('\tCreate clients: OK')
        clientid = str(c.json().get('id'))
        userid = names.get_first_name().lower()
        user = {
            "login": userid,
            "roles": [],
            "clients": [],
            "profile": {
            "name": userid,
            "email": userid+'@'+userid+'.com'
            }
        }
        u = createUserForClient(admin_user, admin_password, clientid, user)
        self.assertEqual(u.status_code, 200)
        print('\tCreate user success: OK')
        u = getUsersForClient(admin_user, admin_password, clientid)
        self.assertEqual(u.status_code, 200)
        self.assertEqual(len(u.json()), 1)
        print('\tNew user is bound to client: OK')
        u = unassigneUserFromClient(admin_user, admin_password, clientid, userid)
        self.assertEqual(u.status_code, 204)
        u = getUsersForClient(admin_user, admin_password, clientid)
        self.assertEqual(u.status_code, 200)
        self.assertEqual(len(u.json()), 0)
        print('\tUser is unassigned from client: OK')
        u = assignUserToClient(admin_user, admin_password, clientid, userid)
        self.assertEqual(u.status_code, 200)
        u = getUsersForClient(admin_user, admin_password, clientid)
        self.assertEqual(u.status_code, 200)
        self.assertEqual(len(u.json()), 1)
        print('\tUser is reassigned: OK')
        u = delUser(admin_user, admin_password, userid)
        self.assertEqual(u.status_code, 204)
        print('\tUser has been deleted: OK')
        cleanup()

    def test_user_permissions(self):
        print('[Test User Permission]')
        c = createClient(names.get_last_name().lower(), '')
        self.assertEqual(c.status_code, 200)
        c = createClient(names.get_last_name().lower(), '')
        self.assertEqual(c.status_code, 200)
        print('\tCreate 2 clients: OK')
        clientid = str(c.json().get('id'))
        userid = names.get_first_name().lower()
        user = {
            "login": userid,
            "roles": [],
            "clients": [],
            "profile": {
            "name": userid,
            "email": userid+'@'+userid+'.com'
            }
        }
        u = createUserForClient(admin_user, admin_password, clientid, user)
        self.assertEqual(u.status_code, 200)
        print('\tCreate user success: OK')
        u = setUserPassword(admin_user, admin_password, userid, userid)
        self.assertEqual(u.status_code, 200)
        print('\tChange user password: OK')
        c = getClients(userid, userid)
        self.assertEqual(c.status_code, 200)
        self.assertEqual(len(c.json()), 1)
        print('\tGet only 1 client for user: OK')
        c = getClients(admin_user, admin_password)
        self.assertEqual(c.status_code, 200)
        self.assertEqual(len(c.json()), 2)
        print('\tTotal client number is 2: OK')
        cleanup()
        
    def test_conversations(self):
        print('[Test Conversation]')
        clientname = names.get_last_name().lower()
        c = createClient(clientname, '')
        self.assertEqual(c.status_code, 200)
        print('\tCreate clients: OK')
        clientid = str(c.json().get('id'))
        config = json.loads('{"queryBuilder":[{"_class":"io.redlink.smarti.model.config.ComponentConfiguration","name":"conversationmlt","displayName":"conversationmlt","type":"conversationmlt","enabled":true,"unbound":false,"pageSize":3,"filter":["support_area"]},{"_class":"io.redlink.smarti.model.config.ComponentConfiguration","name":"conversationsearch","displayName":"conversationsearch","type":"conversationsearch","enabled":true,"unbound":false,"pageSize":3,"filter":["support_area"]}]}')
        c = postConfig(admin_user, admin_password, clientid, config)
        self.assertEqual(c.status_code, 200)
        print('\tPost config for client success: OK')

        channelid = randomID24()
        text = 'Hi, my name is '+names.get_first_name()+' '+names.get_last_name()
        message = {
            "channel_id": channelid,
            "text": text
        }
        conv = postConversation(admin_user, admin_password, clientname, message)
        self.assertEqual(conv.status_code, 200)
        print('\tPost conversation to client success: OK')
        conv = getConversationID(admin_user, admin_password, clientname, channelid)
        self.assertEqual(conv.status_code, 200)
        conversationid = str(conv.text)
        print('\tGet conversationID success: OK')
        conv = publishConversation(admin_user, admin_password, conversationid)
        self.assertEqual(conv.status_code, 200)
        print('\tPublish conversation success: OK')
        conv = getConversationAnalysis(admin_user, admin_password, conversationid)
        self.assertEqual(conv.status_code, 200)
        print('\tGet conversation analysis success: OK')
        
        conv = getConversationResults(admin_user, admin_password, conversationid, '0', 'queryBuilder:conversationmlt:conversationmlt')
        self.assertEqual(conv.status_code, 200)
        print('\tGet conversation results success: OK')
        cleanup()


    def test_performance(self):
        channelnum = 20;
        messagenum = 500;

        print('[Performance Test]')
        print('\tClientNumber: 1')
        print('\tChannelNumber: '+str(channelnum))
        print('\tMessageNumber: '+str(messagenum))

        channelids = set()
        for i in range(channelnum):
            channelids.add(randomID24())
        print('')
        clientname = names.get_last_name().lower()
        c = createClient(clientname, '')
        self.assertEqual(c.status_code, 200)
        clientid = str(c.json().get('id'))
        config = json.loads('{"queryBuilder":[{"_class":"io.redlink.smarti.model.config.ComponentConfiguration","name":"conversationmlt","displayName":"conversationmlt","type":"conversationmlt","enabled":true,"unbound":false,"pageSize":3,"filter":["support_area"]},{"_class":"io.redlink.smarti.model.config.ComponentConfiguration","name":"conversationsearch","displayName":"conversationsearch","type":"conversationsearch","enabled":true,"unbound":false,"pageSize":3,"filter":["support_area"]}]}')
        c = postConfig(admin_user, admin_password, clientid, config)
        self.assertEqual(c.status_code, 200)

        for i in range(messagenum):
            channelid = random.sample(channelids, 1)[0]
            text = randomMessage()
            message = {
                "message_id": randomID24(),
                "channel_id": channelid,
                "text": text
            }
            conv = postConversation(admin_user, admin_password, clientname, message)
            self.assertEqual(conv.status_code, 200)
        
        for channelid in channelids:
            conv = getConversationID(admin_user, admin_password, clientname, channelid)
            self.assertEqual(conv.status_code, 200)
            conversationid = str(conv.text)
            conv = publishConversation(admin_user, admin_password, conversationid)
            self.assertEqual(conv.status_code, 200)
            conv = getConversationAnalysis(admin_user, admin_password, conversationid)
            self.assertEqual(conv.status_code, 200)
            
            conv = getConversationResults(admin_user, admin_password, conversationid, '0', 'queryBuilder:conversationmlt:conversationmlt')
            self.assertEqual(conv.status_code, 200)
        cleanup()


if __name__ == '__main__':
    del sys.argv[1:]
    unittest.main()








