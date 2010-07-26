(ns net.licenser.sandbox.safe-fns
  (:use net.licenser.sandbox.matcher))

(def not-really-safe-functions
    {:java-fns (function-matcher '.)})

(def output-functions
     {:print-fns (function-matcher 'pr 'prn 'print 'println)})

(def safe-functions
     {:internal-fns (function-matcher 'dot)
      :general-fns (function-matcher 
		    '= '== 'case 'if 'comment 'complement 'let 'constantly 
		    'do 'loop* 'loop 'let* 'recur 'fn* 'fn? 'hash 
		    'identical? 'macroexpand  'name 'not= 'partial 
		    'trampoline 'new 'bytes 'assert 'bean 'binding
		    'comp 'complement 'cond 'condp '->> '->
		    'delay 'force 'print-doc 'dotimes 'dtype 'extenders
		    'find-doc 'find-ns 'flush 'format 'gensym
		    'get-method 'hash 'identity
		    'if-let 'if-not 'ifn? 'instance? 'class
		    'clojure-version 'isa? 'juxt() 'keyword 'keyword?
		    'list? 'macroexpand 'macroexpand-1 'memfn 'memoize
		    'methods 'name 'namespace 'pcalls 'pvalues
		    'satisfies? 'special-symbol? 'special-form-anchor
		    'supers 'syntax-symbol-anchor 'the-ns 'type
		    'vary-meta 'when 'when-first 'when-let 'when-not
		    'while 'with-in-str 'with-out-str 'xml-seq 'juxt
		    'throw) 

      :struct-fns (function-matcher 
		   'accessor 'create-struct 'struct 'struct-map)

      :array-fns (function-matcher 
		  'into-array 'aget 'aclone 'alength 'amap 'areduce
		  'array-map 'aset 'aset-boolean 'aset-byte 'aset-char
		  'aset-double 'aset-float 'aset-int 'aset-long 'aset-short
		  'byte-array 'char-array 'constantly 'double-array 
		  'float-array 'int-array 'long-array 'make-array 
		  'object-array 'to-array 'to-array-2d)

      :bit-fns (function-matcher 
		'bit-and-not 'bit-clear 'bit-flip 'bit-not 'bit-or 'bit-set
		'bit-shift-left 'bit-shift-right 'bit-test 'bit-xor)

      :chunk-fns (function-matcher 
		  'chunked-seq? 'chunk-first 'chunk-rest 'chunk-buffer
		  'chunk-append 'chunk-cons 'chunk)
    
      :cast-fns (function-matcher 
		 'int 'char 'long 'short 'symbol 'byte 'boolean
		 'boolean-array 'booleans 'bytes 'cast 'chars 'longs 'bigint
		 'bigdec 'double 'doubles 'enumeration-seq 'float 'floats 
		 'ints 'num 'shorts)

      :string-fns (function-matcher 'subs 'str)

      :regex-fns (function-matcher 
		  're-find 're-groups 're-matcher 're-matches 're-pattern 
		  're-seq)
      
      :meta-fns (function-matcher 'meta 'with-meta)
      
      :logic-fns (function-matcher 
		  'and 'or 'false? 'not 'true? 'char? 'coll? 'float? 
		  'decimal? 'delay? 'integer? 'nil? 'number? 'ratio?
		  'stream? 'string? 'symbol? 'var?)
      
      :math-fns (combine-matchers 
		 (function-matcher 
		  '* '/ '+ '- '< '> '<= '>= 'compare 'dec 'even? 'inc 'max 
		  'min 'mod 'neg? 'odd? 'pos? 'quot 'rand 'rand-int 'rem 
		  'zero? 'with-precision 'max-key 'min-key 'rationalize) 
		 (namespace-matcher 'clojure.contrib.math))

      :clojure-ns (namespace-matcher 'clojure.set 'clojure.stacktrace 'clojure.template
                                     'clojure.test 'clojure.walk 'clojure.xml 'clojure.zip)
      
      :c.c-ns (namespace-matcher 'clojure.contrib.accumulators 'clojure.contrib.apply-macro 'clojure.contrib.base64
                                 'clojure.contrib.combinatorics 'clojure.contrib.complex-numbers 'clojure.contrib.cond
                                 'clojure.contrib.dataflow 'clojure.contrib.datalog 'clojure.contrib.except
                                 'clojure.contrib.fcase 'clojure.contrib.fnmap 'clojure.contrib.generic.arithmetic
                                 'clojure.contrib.generic.collection 'clojure.contrib.generic.comparison
                                 'clojure.contrib.generic.functor 'clojure.contrib.generic.math-functions
                                 'clojure.contrib.graph 'clojure.contrib.greatest-least 'clojure.contrib.json
                                 'clojure.contrib.lazy-seqs 'clojure.contrib.lazy-xml 'clojure.contrib.macro-utils
                                 'clojure.contrib.map-utils 'clojure.contrib.math 'clojure.contrib.mock
                                 'clojure.contrib.monads 'clojure.contrib.probabilities.finite-distributions
                                 'clojure.contrib.probabilities.monte-carlo 'clojure.contrib.probabilities.random-numbers
                                 'clojure.contrib.set 'clojure.contrib.singleton 'clojure.contrib.test-is 'clojure.contrib.trace
                                 'clojure.contrib.types 'clojure.contrib.zip-filter)

      :c.c-ns-1.1 (namespace-matcher 'clojure.contrib.seq-utils  'clojure.contrib.str-utils  'clojure.contrib.str-utils2)
      :c.c-ns-1.2 (namespace-matcher 'clojure.contrib.seq 'clojure.contrib.string)

      :unchecked-math-fns (function-matcher 
			   'unchecked-inc 'unchecked-add 'unchecked-dec
			   'unchecked-divide 'unchecked-multiply
			   'unchecked-negate 'unchecked-remainder
			   'unchecked-subtract)
      
      :coll-fns (function-matcher 
		 'map 'reduce 'count 'doall 'dorun 'doseq 'for
		 'conj 'concat 'cons 'cycle 'interleave 'interpose 'into  
		 'partition 'reverse 'rseq 'seq 'sequence 'sort 'sort-by 
		 'split-at 'split-with 'subseq 'subvec 'tree-seq 'zipmap
		 'vec 'vector 'vector? 'distinct 'drop 'drop-last 'drop-while 
		 'empty 'filter 'not-any? 'not-empty 'not-every? 'remove 
		 'replace 'repeat 'iterate 'list 'list* 'repeatedly 'replicate
		 'range 'ffirst 'first 'fnext 'last 'next 'nfirst 'nnext 'nth 
		 'nthnext 'peek 'pop 'rest 'second 'take 'take-last 'take-nth 
		 'take-while 'contains? 'counted? 'empty? 'every? 'reversible?
		 'seq? 'some 'sorted? 'apply 'butlast 'transient 'vector-of
		 'lazy-seq 'lazy-cat 'mapcat 'pmap 'rsubseq 'sequ 'sequential?
		 'shuffle 'flatten 'group-by 'partition-by 'frequencies
		 'reductions 'partition-all 'rand-nth)
      
      :binding-fns (function-matcher 'push-thread-bindings 'pop-thread-bindings)

      :read-fns (function-matcher 'read-string 'read)
      
      :atom-fns (function-matcher 'atom 'reset! 'swap!)
      
      :ref-fns (function-matcher 
		'deref 'alter 'commute 'dosync 'ensure 'get-validator 
		'ref-set 'set-validator! 'sync 'ref)

      :set-fns (function-matcher 
		'disj 'dissoc 'assoc 'find 'get 'get-in 'hash-set 'hash-map 
		'key 'keys 'merge 'merge-with 'select-keys 'set 'set? 
		'update-in 'sorted-map 'sorted-map-by 'sorted-set 'assoc-in
		'assoc! 'associative? 'vals 'key 'val)

      :clojure-classes (let [c (filter 
				     #(not (nil? %)) 
				     (map #(try (resolve %) (catch Exception e nil))
					  '[clojure.lang.LazySeq clojure.lang.ArrayChunk clojure.lang.XMLHandler
					    clojure.lang.ArraySeq clojure.lang.ChunkBuffer
					    clojure.lang.ChunkedCons clojure.lang.Cons clojure.lang.EnumerationSeq
					    clojure.lang.IndexedSeq clojure.lang.IteratorSeq
					    clojure.lang.Keyword clojure.lang.LazilyPersistentVector
					    clojure.lang.LineNumberingPushbackReader clojure.lang.Numbers
					    clojure.lang.PersistentArrayMap clojure.lang.PersistentHashMap
					    clojure.lang.PersistentHashSet clojure.lang.PersistentList
					    clojure.lang.PersistentQueue clojure.lang.PersistentStructMap
					    clojure.lang.PersistentTreeMap clojure.lang.PersistentTreeSet
					    clojure.lang.PersistentVector clojure.lang.Range clojure.lang.Ratio
					    clojure.lang.SeqEnumeration clojure.lang.SeqIterator clojure.lang.Seqable
					    clojure.lang.Sequential clojure.lang.Sorted clojure.lang.StringSeq
					    clojure.lang.Symbol clojure.lang.TransactionalHashMap
					    clojure.lang.ArrayStream clojure.lang.LazySeq clojure.lang.IteratorStream
					    clojure.lang.Stream clojure.lang.Streamable]))]
			 (if (empty? c)
			   nil
			   (apply class-matcher c)))

      :math-classes (class-matcher java.lang.Number java.lang.Math)
      :basic-classes (class-matcher java.lang.String java.lang.Throwable StringBuilder)
      })

(def save-objects
     {
      :clojure-classes (let [c (filter 
				     #(not (nil? %)) 
				     (map #(try (resolve %) (catch Exception e nil))
					  '[clojure.lang.LazySeq clojure.lang.ArrayChunk clojure.lang.XMLHandler
					    clojure.lang.ArraySeq clojure.lang.ChunkBuffer
					    clojure.lang.ChunkedCons clojure.lang.Cons clojure.lang.EnumerationSeq
					    clojure.lang.IndexedSeq clojure.lang.IteratorSeq
					    clojure.lang.Keyword clojure.lang.LazilyPersistentVector
					    clojure.lang.LineNumberingPushbackReader clojure.lang.Numbers
					    clojure.lang.PersistentArrayMap clojure.lang.PersistentHashMap
					    clojure.lang.PersistentHashSet clojure.lang.PersistentList
					    clojure.lang.PersistentQueue clojure.lang.PersistentStructMap
					    clojure.lang.PersistentTreeMap clojure.lang.PersistentTreeSet
					    clojure.lang.PersistentVector clojure.lang.Range clojure.lang.Ratio
					    clojure.lang.SeqEnumeration clojure.lang.SeqIterator clojure.lang.Seqable
					    clojure.lang.Sequential clojure.lang.Sorted clojure.lang.StringSeq
					    clojure.lang.Symbol clojure.lang.TransactionalHashMap
					    clojure.lang.ArrayStream clojure.lang.LazySeq clojure.lang.IteratorStream
					    clojure.lang.Stream clojure.lang.Streamable]))]
			 (if (empty? c)
			   nil
			   (apply class-matcher c)))

      :math-classes (class-matcher java.lang.Number java.lang.Math)
      :basic-classes (class-matcher java.lang.String java.lang.Throwable StringBuilder Character)
      :clojure-functions (function-matcher 'nth)})

(def bad-objects
     {:clojure-classes (class-matcher clojure.lang.IRef clojure.lang.ARef clojure.lang.Ref)
      :thread-objects (class-matcher java.lang.Thread)})


