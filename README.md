# My Antaeus journey

## @Main
I just included the BillingService in the AntaeusRest Class so that I can access it from the new REST Endpoints created

## @Rest
I just included the BillingService in the AntaeusRest Class so that I can access it from the new REST Endpoints created. 1 new endpoint is created and exposed so that the user can set "on" and "off" the billing service with accessing `/rest/billing/set/{:alive}` and setting `true` or `false` to the respected path parameter. Notice here that by setting true the parameter we trigger a Timer Thread that is running in the background awaiting for the correct moment to settle the payments. By setting false we kill this Timer Thread.

## @BillingService
In the billing service we set a new Timer(Task) daemon Thread that named "PayTimerThread". The Timer Class runs this non-blocking thread in the background and it is set to be triggered on the expected date, thus the 1st (working day) of every month.

The date that the TimerTask is run is found by the internal `getNextBillingDate` function which sets a date on the 1st of the next month at 12:00. Finally it checks if the targeted day is a Saturday or a Sunday and adds to this date 1 or 2 days respectively.

The task to be run at the targetted date is another internal function called `processPayments`. This function "tries" a run-blocking section of code that we need to conclude with no interactions from the Timer Deamon Thread. In this section we do 3 things:
        - call a helper function that emits all Invoices as a Flow
        - filter by valid customerId
        - consume PaymentPRovider.charge API

Finally the function itself sets a new TImer for the next month's payments.

### Extra Information
I have never developed anything in Kotlin and it seems fun :). I will try to break down the effort it took me to do this thingy:

1. Gradle/Docker/Git Config and Tests: ~35mins
2. Read the structure of the project: ~30mins
3. Development: ~4.5h

## --------------------------------------------------------------------------------------------------

## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!
