((clojure-mode . ((eval . (define-key evil-normal-state-map (kbd "ö")
                            '(lambda () (interactive)
                                     (cider-interactive-eval
                                      "(org.motform.merge-podcast-feeds.core/test-merge {:config \"resources/json/example_config.json\"})")))))))
