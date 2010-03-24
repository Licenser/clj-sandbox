(ns net.licenser.sandbox.safe-fns
  (:use net.licenser.sandbox.matchers))

(def safe-functions
     {:variable-fns (function-matcher 'def)

      :general-fns (function-matcher '= '== 'case 'if 'comment 'complement 'let 'constantly 
				     'do 'loop* 'loop 'let* 'recur 'fn* 'fn? 'hash 
				     'identical? 'macroexpand  'name 'not= 'partial 
				     'trampoline 'new '.)

      :chunk-fns (function-matcher 'chunked-seq? 'chunk-first 'chunk-rest)

      :cast-fns (function-matcher 'int)

      :string-fns (function-matcher 'subs 'str)

      :regex-fns (function-matcher 're-find 're-groups 're-matcher 're-matches 're-pattern 
				   're-seq)
      :meta-fns (function-matcher 'meta 'with-meta)

      :logic-fns (function-matcher 'and 'or 'false? 'not 'true?)

      :math-fns (combine-matchers 
		 (function-matcher '* '/ '+ '- '< '> '<= '>= 'compare 'dec 'even? 'inc 'max 
				   'min 'mod 'neg? 'odd? 'pos? 'quot 'rand 'rand-int 'rem 
				   'zero?) 
		 (namespace-matcher 'clojure.contrib.math))

      :unchecked-math-fns (function-matcher 'unchecked-inc)

      :list-fns (function-matcher 'map 'reduce 'count 'doall 'dorun 'doseq
				  'conj 'concat 'cons  'cycle 'interleave 'interpose 'into  
				  'partition 'reverse 'rseq 'seq 'sequence 'sort 'sort-by 
				  'split-at 'split-with 'subseq 'subvec 'tree-seq 'zipmap
				  'vec 'vector 'vector? 'distinct 'drop 'drop-last 'drop-while 
				  'empty 'filter 'not-any? 'not-empty 'not-every? 'remove 
				  'replace 'repeat 'iterate 'list 'list* 'repeatedly 'replicate
				  'range 'ffirst 'first 'fnext 'last 'next 'nfirst 'nnext 'nth 
				  'nthnext 'peek 'pop 'rest 'second 'take 'take-last 'take-nth 
				  'take-while 'contains? 'counted? 'empty? 'every? 'reversible?
				  'seq? 'some 'sorted?)
      
      :set-fns (function-matcher 'disj 'dissoc 'assoc 'find 'get 'get-in 'hash-set 'hash-map 
				 'key 'keys 'merge 'merge-with 'select-keys 'set 'set? 
				 'update-in  'sorted-map 'sorted-map-by 'sorted-set)})