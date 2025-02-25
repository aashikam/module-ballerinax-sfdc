## Overview

Salesforce Sales Cloud is one of the leading Customer Relationship Management(CRM) software, provided by Salesforce.Inc. Salesforce enable users to efficiently manage sales and customer relationships through its APIs, robust and secure databases, and analytics services. Sales cloud provides serveral API packages to make operations on sObjects and metadata, execute queries and searches, and listen to change events through API calls using REST, SOAP, and CometD protocols. 

Ballerina Salesforce connector supports [Salesforce v59.0 REST API](https://developer.salesforce.com/docs/atlas.en-us.224.0.api_rest.meta/api_rest/intro_what_is_rest_api.htm), [Salesforce v59.0 SOAP API](https://developer.salesforce.com/docs/atlas.en-us.api.meta/api/sforce_api_quickstart_intro.htm), [Salesforce v59.0 APEX REST API](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_rest_intro.htm), [Salesforce v59.0 BULK API](https://developer.salesforce.com/docs/atlas.en-us.api_asynch.meta/api_asynch/api_asynch_introduction_bulk_api.htm), and [Salesforce v59.0 BULK V2 API](https://developer.salesforce.com/docs/atlas.en-us.api_asynch.meta/api_asynch/bulk_api_2_0.htm).

## Setup guide

1. Create a Salesforce account with the REST capability.

2. Go to Setup --> Apps --> App Manager 

   <img src=https://raw.githubusercontent.com/ballerina-platform/module-ballerinax-sfdc/master/docs/setup/resources/side-panel.png alt="Setup Side Panel" style="border:1px solid #000000; width:40%">

3. Create a New Connected App.

   <img src=https://raw.githubusercontent.com/ballerina-platform/module-ballerinax-sfdc/master/docs/setup/resources/create-connected-apps.png alt="Create Connected Apps" style="border:1px solid #000000; width:50%">

    - Here we will be using https://test.salesforce.com as we are using sandbox enviorenment. Users can use https://login.salesforce.com for normal usage.

    <img src=https://raw.githubusercontent.com/ballerina-platform/module-ballerinax-sfdc/master/docs/setup/resources/create_connected%20_app.png alt="Create Connected Apps" style="border:1px solid #000000; width:100%">

4. After the creation user can get consumer key and secret through clicking on the `Manage Consume Details` button.

   <img src=https://raw.githubusercontent.com/ballerina-platform/module-ballerinax-sfdc/master/docs/setup/resources/crdentials.png alt="Consumer Secrets" style="border:1px solid #000000; width:100%">

5. Next step would be to get the token.
    - Log in to salesforce in your prefered browser and enter the following url.
  ```
  https://<YOUR_INSTANCE>.salesforce.com/services/oauth2/authorize?response_type=code&client_id=<CONSUMER_KEY>&redirect_uri=<REDIRECT_URL>
  ```
   - Allow access if an alert pops up and the browser will be redirected to a Url like follows.
  
      ```
      https://login.salesforce.com/?code=<ENCODED_CODE>
      ```
  
   - The code can be obtained after decoding the encoded code

6. Get Access and Refresh tokens
   - Following request can be sent to obtain the tokens.
   
      ```
      curl -X POST https://<YOUR_INSTANCE>.salesforce.com/services/oauth2/token?code=<CODE>&grant_type=authorization_code&client_id=<CONSUMER_KEY>&client_secret=<CONSUMER_SECRET>&redirect_uri=https://test.salesforce.com/
      ``` 
   - Tokens can be obtained from the response.

## Quickstart

To use the Salesforce connector in your Ballerina application, modify the .bal file as follows:

#### Step 1: Import connector

Import the `ballerinax/salesforce` package into the Ballerina project.

```ballerina
import ballerinax/salesforce;
```

#### Step 2: Create a new connector instance

Create a `salesforce:ConnectionConfig` with the obtained OAuth2 tokens and initialize the connector with it.
```ballerina
salesforce:ConnectionConfig config = {
    baseUrl: baseUrl,
    auth: {
        clientId: clientId,
        clientSecret: clientSecret,
        refreshToken: refreshToken,
        refreshUrl: refreshUrl
    }
};

salesforce:Client salesforce = new(config);
```

#### Step 3: Invoke connector operation

1. Now you can utilize the available operations. Note that they are in the form of remote operations.  

Following is an example on how to create a record using the connector.

  ```ballerina
  salesforce:CreationResponse response = check 
      baseClient->create("Account", {
                          "Name": "IT World",
                          "BillingCity": "New York"
                          });

  ```

2. Use following command to compile and run the Ballerina program.

```
bal run
````

## Examples

The `salesforce` integration samples illustrate its usage in various integration scenarios. Explore these examples below, covering the use of salesforce APIs in integrations.

1. [FTP B2B EDI message to Salesforce opportunity](https://github.com/ballerina-platform/module-ballerinax-sfdc/tree/main/examples/ftp-edi-message-to-salesforce-opportunity) - This example reads EDI files from a given FTP location, converts those EDI messages to Ballerina records and creates a Salesforce opportunity for each EDI message.

2. [Salesforce new contact to Twilio SMS](https://github.com/ballerina-platform/module-ballerinax-sfdc/tree/main/examples/salesforce-new-contact-to-twilio-sms) - This example sends a Twilio SMS for every new Salesforce contact.

3. [Kafka message to Salesforce new Contact](https://github.com/ballerina-platform/module-ballerinax-sfdc/tree/main/examples/kafka_salesforce_integration/kafka-salesforce-pricebook_update) - This example updates the product price in the Salesforce price book through Kafka and Salesforce integration.

4. [Email Lead info into Salesforce using OpenAI](https://github.com/ballerina-platform/module-ballerinax-sfdc/tree/main/examples/gmail-to-salesforce-lead) - This example creates a lead on Salesforce for each email marked with a specific label on Gmail using the OpenAI chat API to infer customer details.
