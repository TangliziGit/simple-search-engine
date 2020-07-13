EngineActor
    - Done AddRequest(response)
        get doc id
        ! SegDocReq(id, html)
    - Done SearchRequest(word, cb)
        ? SegSWReq(word) -> words
        ! IndexSearchReq(words, cb)

TokenizeActor
    - Done TokenizeDocumentRequest(id, html)
        extract content body
        word segmentation, the result should be: 
        ```
            [
                {keyword: "apple", position: [2, 3, 4]}
            ]
        ```
        ! IndexRequest(id, {content, url, title}, words)
    - Done TokenizeSearchWordRequest(word)
        word segmentation 
        sender ! words

IndexActor
    - Done IndexRequest(id, content, words)
        get hash from content, hash must be average
        ? SCReq(hash, content) -> offset
        engine.indexTable[id] = offset
        merge `engine.invertedIndexTable`, it should be:
        ```
            {
                "word for one doc in Tokenize": [start position of the `word` in doc],
                "word for global docs": {"indics": [(docId, pos of pos), ...], "position": [[start position of the `word` in doc], ...]},
                "apple": {"indics": [(1, 2), (3, 0), (2, 1)], "position": [[2, 3, 4], [1, 2], [4, 5]]}
                ...
            }
        ```
        when engine.invertedIndexTable size(?) > xxx:
            ! FlushInvertedIndexRequst()
        when engine.indexTable size % xxx === 0:
            ! FlushIndexRequest()
    - Done IndexSearchRequest(words, cb)
        for word in words: 
            ! FindInvertedIndexItemRequest(word) -> futureList
        await all futureList
        !!! merge
        calculate BM25
        sort
        replace id into (title, url, content)
        replace content into [first position:XXX]
        cb( [(title, url, content), [positions]] )


StorageActor
    - StoreContentRequest(hash, content)
        fileID <- hash % FileScale
        get file offset
        ! sender offset    ( Future )
        write content to the file 
        close
    - update FlushIndexRequest()
        !!!
    - update FlushInvertedIndexRequst()
        !!!
    - update FindInvertedIndexItemRequest(word)
        !!!
