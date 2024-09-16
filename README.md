# temporal-cloudapi-java-demo
temporal cloud ops api replay 2024 demo using the java sdk

# prereqs
this demo uses an API Key for auth 
the demo assumes the use of an environmental variable for your api key
here's a command that will generate and store the env variable for your api key
TEMPORAL_CLIENT_CLOUD_API_KEY=$(tcld apikey c -n "jltestkey" -d "30d" | jq -r ".secretKey")
the command assumes you have succefully authenticated into tcld using tcld login