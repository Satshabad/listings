# Listings API

If I were to spend more time on this I would

    - read the actual RFC and make sure to do pagination right.
    - pass the original query params back in the pagination.
    - proper error messages for wrong query params.
    - open source that pagination library in Clojure because nothing like it exists yet.
    - perform a security audit, as of now there's probably ways to do nasty things.
    - probably switch to a higher level sql library to not shoot myself in the foot.
    - do perf testing and see if I can do pagination faster.
    - write docs, maybe live docs with something like swagger.
    - write unit tests
    - Add sq_foot and geographical filters
