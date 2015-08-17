# Stress Service

This project is forked from [akka-http-microservice](https://github.com/theiterators/akka-http-microservice).
The project is created for stress test.

[![Deploy to Heroku](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy)

## Usage

Start services with sbt:

```
$ sbt
> ~re-start
```

With the service up, you can start sending HTTP requests:

```
$ curl http://localhost:8080/strong/2015-08-17
{
  "message": "find date: 2015-08-17",
  "interval": 230
}
```

### Testing

Execute tests using `test` command:

```
$ sbt
> test
```

## Author & license

x1- <viva008@gmail.com> from http://x1.inkenkun.com.