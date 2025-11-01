import requests # 오류가 있으면 “pip install requests”

HOST = 'http://127.0.0.1:8000'
res = requests.post(HOST + '/api-token-auth/', {
    'username’:’admin',
    'password’:’admin',
})

res.raise_for_status()
token = res.json()['token']
print(token)

# 인증이 필요한 요청에 아래의 headers를 붙임
headers = {'Authorization' : 'JWT ' + token, 'Accept' : 'application/json'}

# Post Create
data = {
    'title' : '제목 by code',
    'text' : 'API내용 by code',
    'created_date' : '2024-06-03T18:34:00+09:00',
    'published_date' : '2024-06-03T18:34:00+09:00'
}

file = {'image' : open('/Users/kkddhh/Desktop/else/IMG_0017.jpeg', 'rb')}
res = requests.post(HOST + '/api_root/Post/', data=data, files=file, headers=headers)
print(res)
print(res.json())