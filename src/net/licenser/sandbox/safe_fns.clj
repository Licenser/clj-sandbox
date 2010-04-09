(ns net.licenser.sandbox.safe-fns
  (:use net.licenser.sandbox.matcher))

(def not-really-safe-functions
    {:java-fns (function-matcher '.)})

(def safe-functions
     {:general-fns (function-matcher 
		    '= '== 'case 'if 'comment 'complement 'let 'constantly 
		    'do 'loop* 'loop 'let* 'recur 'fn* 'fn? 'hash 
		    'identical? 'macroexpand  'name 'not= 'partial 
		    'trampoline 'new 'bytes '->> '-> 'assert 'bean 'binding
		    'comp 'complement 'cond 'condp)
      :general-fns-l nil
      :general-fns-r nil

      :struct-fns (function-matcher 'accessor 'create-struct)
      :struct-fns-l nil
      :struct-fns-r nil

      :array-fns (function-matcher 
		  'into-array 'aget 'aclone 'alength 'amap 'areduce
		  'array-map 'aset 'aset-boolean 'aset-byte 'aset-char
		  'aset-double 'aset-float 'aset-int 'aset-long 'aset-short
		  'byte-array 'char-array 'class 'clojure-version
		  'constantly)
      :array-fns-l nil
      :array-fns-r nil

      :bit-fns (function-matcher 
		'bit-and-not 'bit-clear 'bit-flip 'bit-not 'bit-or 'bit-set
		'bit-shift-left 'bit-shift-right 'bit-test 'bit-xor)
      :bit-fns-l nil
      :bit-fns-r nil

      :chunk-fns (function-matcher 
		  'chunked-seq? 'chunk-first 'chunk-rest 'bigint 'bigdec)
      :chunk-fns-l nil
      :chunk-fns-r nil
      
      :cast-fns (function-matcher 
		 'int 'char 'long 'short 'symbol 'byte 'boolean
		 'boolean-array 'booleans 'bytes 'cast 'chars)
      :cast-fns-l nil
      :cast-fns-r nil

      :string-fns (function-matcher 'subs 'str)
      :string-fns-l nil
      :string-fns-r nil

      :regex-fns (function-matcher 're-find 're-groups 're-matcher 're-matches 're-pattern 
				   're-seq)
      :regex-fns-l nil
      :regex-fns-r nil
      
      :meta-fns (function-matcher 'meta 'with-meta)
      :meta-fns-l nil
      :meta-fns-r nil
      
      :logic-fns (function-matcher 'and 'or 'false? 'not 'true? 'char? 'coll?)
      :logic-fns-l nil
      :logic-fns-r nil
      
      :math-fns (combine-matchers 
		 (function-matcher 
		  '* '/ '+ '- '< '> '<= '>= 'compare 'dec 'even? 'inc 'max 
		  'min 'mod 'neg? 'odd? 'pos? 'quot 'rand 'rand-int 'rem 
		  'zero?) 
		 (namespace-matcher 'clojure.contrib.math))
      :math-fns-l nil
      :math-fns-r nil
      
      :unchecked-math-fns (function-matcher 'unchecked-inc)
      :unchecked-math-fns-l nil
      :unchecked-math-fns-r nil
      
      :list-fns (function-matcher 
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
		 'seq? 'some 'sorted? 'apply 'butlast)
      :list-fns-l nil
      :list-fns-r nil
      
      :binding-fns (function-matcher 'push-thread-bindings 'pop-thread-bindings)
      :binding-fns-l nil
      :binding-fns-r nil

      :read-fns (function-matcher 'read-string 'read)
      :read-fns-l nil
      :read-fns-r nil
      
      :atom-fns (function-matcher 'atom)
      :atom-fns-l nil
      :atom-fns-r nil
      
      :ref-fns (function-matcher 'deref 'alter 'commute)
      :ref-fns-l nil
      :ref-fns-r nil

      :set-fns (function-matcher 
		'disj 'dissoc 'assoc 'find 'get 'get-in 'hash-set 'hash-map 
		'key 'keys 'merge 'merge-with 'select-keys 'set 'set? 
		'update-in 'sorted-map 'sorted-map-by 'sorted-set 'assoc-in
		'assoc! 'associative? )
      :set-fns-l nil
      :set-fns-r nil})

