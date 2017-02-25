### Spring Data REST multiple repositories workaround

##### Repositories are secured by role:
```
$ curl will:faithfull@localhost:8080/posts
{
  "timestamp" : "2017-02-25T12:13:49.468+0000",
  "status" : 403,
  "error" : "Forbidden",
  "exception" : "org.springframework.security.access.AccessDeniedException",
  "message" : "Access is denied",
  "path" : "/posts"
}
```
```
$ curl oliver:gierke@localhost:8080/posts
{
  "_embedded" : {
    "posts" : [ {
      "title" : "Hello World",
      "content" : "This is demonstrating an ugly hack to allow both an exported and internal repository.",
      "_links" : {
        "self" : {
          "href" : "http://localhost:8080/posts/1"
        },
        "post" : {
          "href" : "http://localhost:8080/posts/1"
        }
      }
    } ]
  },
  "_links" : {
    "self" : {
      "href" : "http://localhost:8080/posts"
    },
    "profile" : {
      "href" : "http://localhost:8080/profile/posts"
    }
  },
  "page" : {
    "size" : 20,
    "totalElements" : 1,
    "totalPages" : 1,
    "number" : 0
  }
}
```

##### For-internal-use repositories are not secured.
```
$ curl will:faithfull@localhost:8080/bypass/
{
  "_embedded" : {
    "posts" : [ {
      "title" : "Hello World",
      "content" : "This is demonstrating an ugly hack to allow both an exported and internal repository.",
      "_links" : {
        "self" : {
          "href" : "http://localhost:8080/posts/1"
        },
        "post" : {
          "href" : "http://localhost:8080/posts/1"
        }
      }
    } ]
  },
  "_links" : {
    "self" : {
      "href" : "http://localhost:8080/bypass/"
    }
  }
}
```
```
curl oliver:gierke@localhost:8080/bypass/
{
  "_embedded" : {
    "posts" : [ {
      "title" : "Hello World",
      "content" : "This is demonstrating an ugly hack to allow both an exported and internal repository.",
      "_links" : {
        "self" : {
          "href" : "http://localhost:8080/posts/1"
        },
        "post" : {
          "href" : "http://localhost:8080/posts/1"
        }
      }
    } ]
  },
  "_links" : {
    "self" : {
      "href" : "http://localhost:8080/bypass/"
    }
  }
}
```
