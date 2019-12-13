templated-mail-sender
==========================

A simple library for sending templated, localized emails. The email templates themselves are stored locally
on disk (and can be in git). Designed to be easily embedded within a JVM environment.

## Sending a message directly via the message queue

If you know where a mail-sender is running, you can craft a JSON message and put it directly onto the queue.

Simply issue a SET command against the kestrel server and use the following exemplary JSON as a guide:

    {
      "event": "queue_tmail"      <!-- required, must be queue_tmail or message will be rejected -->
      "message": {
        "templateName" : "test_template",   <!-- required, refers to a set of files (possibly localized), located in
                                                 the emailTemplateBaseDir, which is configured in the YML config -->
        "toName" : "Some User"              <!-- optional, default is to leave empty -->
        "toEmail" : "user@example.com"      <!-- required -->
        "locale" : "fr_fr"                  <!-- optional, default is en_us -->
        "parameters" : {                    <!-- optional, and as shown here, you can use map-style values for objects -->
          "someParam" : "Some Value"
          "anotherParam" : "Another Value"
          "user" : {
            "id": "some-user-id"
            "name": "some-user-name"
          }
        }
      }
    }


## Running a client locally within the JVM

### Install the templated-mail-sender jar locally:

    cd /where/you/code
    git clone git@github.com:cobbzilla/templated-mail-sender.git
    cd templated-mail-sender
    mvn clean package install

### Include this maven dependency in your pom.xml

    <dependency>
      <groupId>org.cobbzilla</groupId>
       <artifactId>templated-mail-sender</artifactId>
       <version>1.0.0-SNAPSHOT</version>
    </dependency>

### Initialize a client object

    final TemplatedMailConfiguration mailConfiguration = new TemplatedMailConfiguration();
    ... set various things on the configuration, or initialize from yml  ...

    mailClient = new TemplatedMailClient(mailConfiguration);
    mailClient.init();

### Send an email from Java

    Map<String, String> params = new HashMap<>();
    params.put("testParam", "testValue");

    Map<String, String> user = new HashMap<>();
    user.put("id", "some-user-id");
    user.put("name", "some-user-name");
    params.put("user", user);

    TemplatedMail testMessage = new TemplatedMail();
    testMessage.setTemplateName("test_template");
    testMessage.setToEmail("recipient@example.com");
    testMessage.setLocale("en_us");
    testMessage.setEmailMessage(emailMessage);
    testMessage.setParameters(params);
    mailClient.send(testMessage);

## Creating a new email template

In your emailTemplateBaseDir directory (use TemplatedMailConfiguration.setEmailTemplateBaseDir or initialize via yml)
 create a new directory for your email. Let's say the emailTemplateBaseDir is /home/tout/email_templates and we want
 to create a new template named "foo/mail". We'll need to define, at a minimum:

    /home/tout/email_templates/foo/mail.fromEmail.mustache  (the "from" email)
    /home/tout/email_templates/foo/mail.subject.mustache  (the subject line)
    /home/tout/email_templates/foo/mail.textMessage.mustache  (for mail clients that do not support HTML email)

Optionally, we can also specify:

    /home/tout/email_templates/foo/mail.fromName.mustache  (the "from" name)
    /home/tout/email_templates/foo/mail.cc.mustache  (a "cc" email, just one)
    /home/tout/email_templates/foo/mail.bcc.mustache  (a "bcc" email, just one)
    /home/tout/email_templates/foo/mail.htmlMessage.mustache  (for mail clients that DO support HTML email)

The contents of the above files are mustache templates. Please see http://mustache.github.com/mustache.5.html
for more information on how to use mustache. Within the templates, you can reference any attributes that you
expect to receive in the message.parameters that clients will send along with their requests to queue emails.

## Localizing an email template

Create versions that include the localization string (use all lowercase), for example:

    /home/tout/email_templates/foo/mail.textMessage_en_us.mustache
    /home/tout/email_templates/foo/mail.textMessage_fr_fr.mustache
    /home/tout/email_templates/foo/mail.textMessage_fr.mustache

The template system will find the best match it can. For example:

* If the user's locale is "fr\_fr" (French, France), they will get the mail.textMessage\_fr\_fr.mustache version
* If the user's locale is "fr\_ca" (French, Canada), they will get the mail.textMessage\_fr.mustache version
* If the user's locale is "es\_es" (Spanish, Spain), they will get the mail.textMessage.mustache version (default version), since no other locale-specific templates match.

