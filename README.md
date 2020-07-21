# Simple Search Engine

To learn general architecture and details of search engine.  
Want <u>***your star***</u> to satisfy my little vanity ^\_^.  

开发结束啦，有空再认真写写README，详细文档请见`技术文档`

**需要借鉴请star**


## Architecture

Here should be an architecture image.

- Engine: manager of whole search engine
- Tokenizer: to tokenize (segment) Chinese sentences in documents and search sentence into words 
- Indexer: to build index table and inverted index table of all documents, rank document relevance and sort
- Storage: to store tables into disk and manage cache
- Crawler: crawl pages from seed page or maintain a url set

## Detail

1. actor model design (for highly async)
2. sync in actor system (using Future)
3. file structure of inverted index table (for quickly reading)
4. speed up searching documents (data structure)
5. rank algorithm
6. crawler 
7. maybe distribution? (actor model's advantage over CSP model)

## License

> MIT License
>
> Copyright (c) 2020 Chunxu Zhang

