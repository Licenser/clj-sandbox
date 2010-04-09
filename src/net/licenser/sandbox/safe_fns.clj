(ns net.licenser.sandbox.safe-fns
  (:use net.licenser.sandbox.matcher))

(def not-really-safe-functions
    {:java-fns (function-matcher '.)})

(def safe-functions
     {:general-fns (function-matcher 
		    '= '== 'case 'if 'comment 'complement 'let 'constantly 
		    'do 'loop* 'loop 'let* 'recur 'fn* 'fn? 'hash 
		    'identical? 'macroexpand  'name 'not= 'partial 
		    'trampoline 'new 'bytes 'assert 'bean 'binding
		    'comp 'complement 'cond 'condp '->> '->
		    'delay 'force 'print-doc 'dotimes 'dtype 'extenders
		    'find-doc 'find-ns 'find-var 'flush 'format 'gensym
		    'get-method 'get-thread-bindings 'hash 'identity
		    'if-let 'if-not 'ifn? 'instance? 'class
		    'clojure-version 'isa? 'juxt 'keyword 'keyword?
		    'list? 'macroexpand 'macroexpand-1 'memfn 'memoize
		    'methods 'name 'namespace 'pcalls 'pvalues
		    'satisfies? 'special-symbol? 'special-form-anchor
		    'supers 'syntax-symbol-anchor 'the-ns 'type
		    'vary-meta 'when 'when-first 'when-let 'when-not
		    'while 'with-in-str 'with-out-str 'xml-seq) 

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

      :chunk-fns (function-matcher 'chunked-seq? 'chunk-first 'chunk-rest)
      
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
      
      :unchecked-math-fns (function-matcher 
			   'unchecked-inc 'unchecked-add 'unchecked-dec
			   'unchecked-divide 'unchecked-multiply
			   'unchecked-negate 'unchecked-remainder
			   'unchecked-subtract)
      
      :coll-fns (function-matcher 
		 'map 'reduce 'count 'doall 'dorun 'doseq
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
		 'lazy-seq 'lazy-cat 'mapcat 'pmap 'rsubseq 'sequ 'sequential?)
      
      :binding-fns (function-matcher 'push-thread-bindings 'pop-thread-bindings)

      :read-fns (function-matcher 'read-string 'read)
      
      :atom-fns (function-matcher 'atom 'reset! 'swap!)
      
      :ref-fns (function-matcher 
		'deref 'alter 'commute 'dosync 'ensure 'get-validator 
		'ref-set 'reset-meta! 'set-validator! 'sync)

      :set-fns (function-matcher 
		'disj 'dissoc 'assoc 'find 'get 'get-in 'hash-set 'hash-map 
		'key 'keys 'merge 'merge-with 'select-keys 'set 'set? 
		'update-in 'sorted-map 'sorted-map-by 'sorted-set 'assoc-in
		'assoc! 'associative? 'vals)})

