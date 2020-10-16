# My Antaeus journey

## @Main
1. I included the BillingService in the AntaeusRest Class so that I can access it from the new REST Endpoints created
2. I included CustomerService and InvoiceService as Class variables of BillingService

## @AntaeusRest Class

New endpoints have been created and exposed in the `AntaeusRest.kt` file. Specifically, the following have been added to the project:
- `/rest/v1/payments/paid` > Gets all the PAID Invoices
- `/rest/v1/payments/pending` > Gets all the PENDING Invoices
- `/rest/billing/set/{:alive}` > This endpoint accepts a `Boolean` parameter so that billing scheduler is activated or deactivated based to its respected value.
- `/rest/billing/force` > Re-tries to charge all PENDING Invoices
- `/rest/billing/force/{:id}` > Charges a particular Invoice by setting the respective `Invoice.id` as a path parameter

## @Scheduler Class (New)
`package io.pleo.antaeus.core.util`

This is my Scheduler Class that does 2 main things
- It manages the `Timer` deamon thread so that always 1 is active. The deamon thread can be started or stopped by the `start` or `stop` functions.
- It calculates the next Billing Date utilizing the `getNextBillingDate` function. Always, the next billing date set is the first working day of the next month, so that the billing is avoided during weekends.

## @CoreLogger Class (New) 
 `package io.pleo.antaeus.core.util`

This class delivers by custom loggin function so that logging happens with a certain pattern. The methods include:
- `log`   > For debugging messages
- `info`  > For informative messages
- `error`  > For error messages


## @BillingService

The billing service then is basically just consumes the Scheduler API and offers the billing processing logic as a whole. All in all, the billing service exposes 3 main functions to support the requirements:

- `fun schedulePays(alive: Boolean) : BSResult` This function enables or disables the Scheduler based on the value of the `alive` parameter. When `true`, the logic starts a Scheduling task using its private Scheduler object so that, all billing invoices are charged on next month's 1st working day and then a new scheduling task is automatically triggered for the month after etc. The function returns a custom JSON Object with the following format:

```
BSResult : {
        nextBillDate : Date     Date of the next monthly billing process
        cronEvtSet   : Boolean  Flag that shows if the Scheduler is set  
}
```

- `fun schedulePays(alive: Boolean) : BSResult` This function enables or disables the Scheduler based on the value of the `alive` parameter. When `true`, the logic starts a Scheduling task using its private Scheduler object so that: A. all billing invoices are charged on next month's 1st working day and B. a recurring payment is automatically setup for the month after. 

- `fun forcePendingPays() : Boolean` This function sends for processing only the Invoices with a 'PENDING' status. It returns `true` if there are still pending/failing payments (to charge later by Pleo) and `false` if all current invoices are 'PAID'

- `fun processPayment(invoice: Invoice) : Boolean` This function sends a single Invoice for charging. It is used internally by all `schedulePays` and `forcePendingPays`, in case a single payment is to be managed ever. It returns `true` if the charge go through successfully and `false` if any errors occur. 

- `fun processPayment(invoiceId: Int) : Boolean` *Overloaded* function to give the chance to the REST API consumer to process a payment by invoice ID. Exactly the same as above. It is used by the REST endpoint `/rest/billing/force/{:id}` shown above.

## Unit Testing 
### BillingServiceTest.kt
`package io.pleo.antaeus.core.services`

- Tests BillingService functions
- Ensures that BillingService is aware of any failing payments handling successfully any occuring errors


### SchedulerTest.kt
`package io.pleo.antaeus.core.services`

- Ensures the Scheduling API works correctly
- Ensures 1 Timer Thread is created per instance

## Error Handling
- All possibly occuring errors are caught and logged via the CoreLogger.
- Any charging process is made sure it is not interrupted by any error.
- A new `PaymentsProcessException` Class is created so that in order to identify potential errors while running the BillingService core logic.


### WalkThrough & Extra Information
I have never developed anything in Kotlin and it seems fun :). I will try to break down the effort/thoughts and walk you through all my thoughts about this project.

Initially after reading the requirement and seeing the Invoices I could not really tell for sure if these invoices are transactions that the clients have done with your product (ie. with the Pleo card) or if each Invoice refers to fees that Pleo needs to charge from the respective customers. 

The problem here is that if the 1st case is true I would need to get a fee from each pay and then accumulatively charge the customer ie. since 1 customer has 1 Currency in all invoices, I would reduce the grouped-by-customer Invoices into 1 Invoice per customer like in the code below: 

```
// A PIECE OF CODE THAT WAS NEVER MEANT TO WORK
private fun emitCustomerInvoices() = Flow {
        customerService.fetch().forEach { customer -> {
                val invoice = invoiceService.fetchAll()
                    .sortedBy(it.customerId)
                    .filter { it.customerId == customer.id}
                    .fold(Invoice()) { 
                        (accInvoice, next) -> {
                                accInvoice(
                                        customer.id,
                                        customer.id, 
                                        Money(BigDecimal( accInvoice.money.amount + next.money.amount),Currency.DKK),   // <---- Add amounts like that
                                        InvoiceStatus.PAID                                                              // <---- Problem status has multiple values per customer
                                )
                                emit(accInvoice)
                        }
                    }
                    emit(invoice)
        }   
}
```

So I moved to the next case because of the multiple invoice statuses the problem got bigger and bigger. What I did then was to generate a Scheduler. 

After some research I found out that I could solve the Scheduling problem with a JAVA TimerTask. Thus, I created a Schedule Class that will manage the Timer Thread within this instance's lifecycle. The Schedule class it giving an API to trigger, stop and monitor this Timer thread outside of this class. 

Then I started developing the BillingService logic having in mind what endpoints I would need to expose or I would like to expose. I though that Pleo would most probably need these features below:

- A scheduling task for the 1st working day of the month
- A recurring event of this task (same every month)
- An endpoint to force failed Invoices to be charged
- An endpoint to force singular Invoices to process

### Future Improvements & Miscellaneous
1. The scheduling design in the app here is not very good. Since the instance can be loaded multiple times (Docking), it is dodgy to setup the Scheduling Logic here. Instead, it should be in a third party system (for example it could be a cron event on AWS or similar)
2. I would try if I do Antaeus again to persistently Queue the payments, so that I could dedeque (ie. poll()) the successfully paid ones and handle the rest when re-trying to charge the PENDING Invoices of every month. 

### Time Estimations
1. Gradle/Docker/Git Config and Tests: ~35mins
2. Read the structure of the project: ~1h
3. Development: ~7.5h
4. Unit Tests: ~3h
5. Documentation/README/JavaDoc: ~4h


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
