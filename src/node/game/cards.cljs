(ns game.cards
  (:require-macros [game.macros :refer [effect req msg]])
  (:require [game.core :refer [pay gain lose draw move damage shuffle-into-deck trash purge add-prop
                               set-prop resolve-ability system-msg end-run unregister-event mill run
                               gain-agenda-point pump access-bonus] :as core]
            [game.utils :refer [has?]]))

(def cards
  {"Access to Globalsec"
   {:effect (effect (gain :link 1)) :leave-play (effect (lose :link 1))}

   "Adonis Campaign"
   {:data {:counter 12}
    :events {:corp-turn-begins {:msg "gain 3 [Credits]" :counter-cost 3
                                :effect #(do (gain %1 :corp :credit 3)
                                             (when (zero? (:counter %3)) (trash %1 :corp %3)))}}}

   "Akamatsu Mem Chip"
   {:effect (effect (gain :memory 1)) :leave-play (effect (lose :memory 1))}

   "All-nighter"
   {:abilities [{:cost [:click 1] :effect (effect (trash card) (gain :click 2))
                 :msg "gain [Click][Click]"}]}

   "Armitage Codebusting"
   {:data {:counter 12}
    :abilities [{:cost [:click 1] :counter-cost 2 :msg "gain 2 [Credits]"
                 :effect #(do (gain %1 :runner :credit 2)
                              (when (zero? (:counter %3)) (trash %1 :runner %3)))}]}

   "Andromeda: Dispossessed Ristie"
   {:effect (effect (gain :link 1) (draw 4))}

   "Anonymous Tip"
   {:effect (effect (draw 3))}

   "Astrolabe"
   {:effect (effect (gain :memory 1)) :leave-play (effect (lose :memory 1))
    :events {:server-created {:msg "draw 1 card" :effect (effect (draw :runner))}}}

   "Autoscripter"
   {:events {:runner-install {:req (req (and (= (:active-player @state) :runner)
                                             (has? target :type "Program")))
                              :once :per-turn :msg "gain [Click]" :effect (effect (gain :click 1))}
             :unsuccessful-run {:effect (effect (trash card)
                                                (system-msg "Autoscripter is trashed"))}}}

   "Beanstalk Royalties"
   {:effect (effect (gain :credit 3))}

   "Blue Level Clearance"
   {:effect (effect (gain :credit 5) (draw 2))}

   "Big Brother"
   {:req (req tagged) :effect (effect (gain :runner :tag 2))}

   "Biotic Labor"
   {:effect (effect (gain :click 2))}

   "Borrowed Satellite"
   {:effect (effect (gain :link 1 :max-hand-size 1))
    :leave-play (effect (lose :link 1 :max-hand-size 1))}

   "BOX-E"
   {:effect (effect (gain :memory 2 :max-hand-size 2))
    :leave-play (effect (lose :memory 2 :max-hand-size 2))}

   "Cache"
   {:abilities [{:counter-cost 1 :effect (effect (gain :credit 1)) :msg "gain 1 [Credits]"}]
    :data {:counter 3}}

   "Calling in Favors"
   {:effect (effect (gain :credit (count (filter (fn [c] (has? c :subtype "Connection"))
                                                 (get-in runner [:rig :resource])))))}

   "Cerebral Imaging: Infinite Frontiers"
   {:effect #(add-watch % :cerebral-imaging
                        (fn [k ref old new]
                          (let [credit (get-in new [:corp :credit])]
                            (when (not= (get-in old [:corp :credit]) credit)
                              (swap! ref assoc-in [:corp :max-hand-size] credit)))))}

   "Chairman Hiro"
   {:effect (effect (lose :runner :max-hand-size 2))
    :leave-play (effect (gain :runner :max-hand-size 2))
    :trash-effect {:req (req access)
                   :effect (effect (move :runner card :scored) (gain :runner :agenda-point 2))}}

   "Chaos Theory: Wünderkind"
   {:effect (effect (gain :memory 1))}

   "Chronos Project"
   {:effect (effect (move :runner :discard :rfg))}

   "Clone Retirement"
   {:msg "remove 1 bad publicity" :effect (effect (lose :bad-publicity 1))
    :stolen {:msg "force the Corp to take 1 bad publicity"
             :effect (effect (gain :corp :bad-publicity 1))}}

   "Closed Accounts"
   {:req (req tagged) :effect (effect (lose :runner :credit :all))}

   "Corporate Shuffle"
   {:effect (effect (shuffle-into-deck :hand) (draw 5))}

   "Corporate War"
   {:effect #(if (> (get-in @% [:corp :credit]) 6)
               (gain % :corp :credit 7)
               (lose % :corp :credit :all))}

   "Cyberdex Trial"
   {:effect (effect (purge))}

   "CyberSolutions Mem Chip"
   {:effect (effect (gain :memory 2)) :leave-play (effect (lose :memory 2))}

   "Daily Casts"
   {:data {:counter 8}
    :events {:runner-turn-begins {:msg "gain 2 [Credits]" :counter-cost 2
                                  :effect #(do (gain %1 :runner :credit 2)
                                               (when (zero? (:counter %3)) (trash %1 :runner %3)))}}}

   "Data Leak Reversal"
   {:req (req (some #{:hq :rd :archives} (:successful-run runner-reg)))
    :abilities [{:req (req tagged) :cost [:click 1] :effect (effect (mill :corp))}]}

   "Dedicated Response Team"
   {:events {:successful-run-ends {:req (req tagged) :msg "do 2 meat damages"
                                   :effect (effect (damage :meat 2))}}}

   "Desperado"
   {:effect (effect (gain :memory 1)) :leave-play (effect (lose :memory 1))
    :events {:successful-run {:msg "gain 1 [Credits]" :effect (effect (gain :credit 1))}}}

   "Diversified Portfolio"
   {:effect (effect (gain :credit (count (get-in corp [:servers :remote]))))}

   "Diesel"
   {:effect (effect (draw 3))}

   "Director Haas"
   {:effect (effect (gain :click 1 :click-per-turn 1)) :leave-play (effect (lose :click-per-turn 1))
    :trash-effect {:req (req access)
                   :effect (effect (move :runner card :scored) (gain :runner :agenda-point 2))}}

   "Domestic Sleepers"
   {:abilities [{:cost [:click 3]
                 :effect #(do (when (zero? (:counter %3)) (gain-agenda-point %1 %2 1))
                              (set-prop %1 %2 %3 :counter 1 :agendapoints 1))}]}

   "Duggars"
   {:abilities [{:cost [:click 4] :effect (effect (draw 10)) :msg "draw 10 card"}]}

   "Dyson Mem Chip"
   {:effect (effect (gain :link 1 :memory 1)) :leave-play (effect (lose :link 1 :memory 1))}

   "Easy Mark"
   {:effect (effect (gain :credit 3))}

   "Ekomind"
   {:effect #(do (swap! %1 assoc-in [:runner :memory] (count (get-in @%1 [:runner :hand])))
                 (add-watch % :ekomind (fn [k ref old new]
                                         (let [hand-size (count (get-in new [:runner :hand]))]
                                           (when (not= (count (get-in old [:runner :hand])) hand-size)
                                             (swap! ref assoc-in [:runner :memory] hand-size))))))
    :leave-play #(remove-watch % :ekomind)}

   "Eve Campaign"
   {:data {:counter 16}
    :events {:corp-turn-begins {:msg "gain 3 [Credits]" :counter-cost 2
                                :effect #(do (gain %1 :corp :credit 2)
                                             (when (zero? (:counter %3)) (trash %1 :corp %3)))}}}

   "Executive Retreat"
   {:data {:counter 1} :effect (effect (shuffle-into-deck :hand))
    :abilities [{:cost [:click 1] :counter-cost 1 :msg "draw 5 cards" :effect (effect (draw 5))}]}

   "Fetal AI"
   {:access {:req (req (not= (:zone card) :archives)) :msg "do 2 net damages"
             :effect (effect (damage :net 2))}
    :steal-cost [:credit 2]}

   "Fester"
   {:events {:purge {:msg "force the corp to lose 2 [Credits] if able"
                     :effect (effect (pay :corp :credit 2))}}}

   "Gabriel Santiago: Consummate Professional"
   {:events {:successful-run {:msg "gain 2 [Credits]" :once :per-turn
                              :effect (effect (gain :credit 2)) :req (req (= target :hq))}}}

   "Geothermal Fracking"
   {:data {:counter 2}
    :abilities [{:cost [:click 1] :counter-cost 1 :msg "gain 7 [Credits] and take 1 bad publicity"
                 :effect (effect (gain :credit 7 :bad-publicity 1))}]}

   "Gila Hands Arcology"
   {:abilities [{:cost [:click 2] :effect (effect (gain :credit 3)) :msg "gain 3 [Credits]"}]}

   "Gorman Drip v1"
   {:abilities [{:cost [:click 1] :effect (effect (gain :credit (:counter card)) (trash card))
                 :msg (msg "gain " (:counter card) " [Credits]")}]
    :events {:corp-click-credit {:effect (effect (add-prop card :counter 1))}
             :corp-click-draw {:effect (effect (add-prop card :counter 1))}}}

   "Government Contracts"
   {:abilities [{:cost [:click 2] :effect (effect (gain :credit 4)) :msg "gain 4 [Credits]"}]}

   "Green Level Clearance"
   {:effect (effect (gain :credit 3) (draw))}

   "Grimoire"
   {:effect (effect (gain :memory 2)) :leave-play (effect (lose :memory 2))
    :events {:runner-install {:req (req (has? target :subtype "Virus"))
                              :effect (effect (add-prop target :counter 1))}}}

   "GRNDL: Power Unleashed"
   {:effect (effect (gain :credit 5 :bad-publicity 1))}

   "GRNDL Refinery"
   {:advanceable :always
    :abilities [{:cost [:click 1] :effect (effect (gain :credit (* 4 (:advance-counter card))) (trash card))}]}

   "Haas Arcology AI"
   {:advanceable :while-unrezzed
    :abilities [{:cost [:click 1] :advance-counter-cost 1 :effect (effect (gain :click 2))}]}

   "Haas-Bioroid: Engineering the Future"
   {:events {:corp-install {:once :per-turn :msg "gain 1 [Credits]"
                            :effect (effect (gain :credit 1))}}}

   "Hard at Work"
   {:events {:runner-turn-begins {:msg "gain 2 [Credits] and lose [Click]"
                                  :effect (effect (lose :click 1) (gain :credit 2))}}}

   "Harmony Medtech: Biomedical Pioneer"
   {:effect (effect (lose :agenda-point-req 1) (lose :runner :agenda-point-req 1))}

   "Hedge Fund"
   {:effect (effect (gain :credit 9))}

   "High-Risk Investment"
   {:data {:counter 1}
    :abilities [{:cost [:click 1] :counter-cost 1 :msg (msg "gain" (:credit runner) " [Credits]")
                 :effect (effect (gain :credit (:credit runner)))}]}

   "Hostile Takeover"
   {:effect (effect (gain :credit 7 :bad-publicity 1))}

   "House of Knives"
   {:data {:counter 3}
    :abilities [{:counter-cost 1 :msg "do 1 net damage" :req (req (:run @state)) :once :per-run
                 :effect (effect (damage :net 1))}]}

   "Human First"
   {:events {:agenda-scored {:msg (msg "gain " (:agendapoints target) " [Credits]")
                             :effect (effect (gain :runner :credit (:agendapoints target)))}
             :agenda-stolen {:msg (msg "gain " (:agendapoints target) " [Credits]")
                             :effect (effect (gain :credit (:agendapoints target)))}}}

   "Hostile Infrastructure"
   {:events {:trash {:req (req (and (= (:side target) :corp) (= side :runner)))
                     :effect (effect (damage :net 1))}}}

   "HQ Interface"
   {:effect (effect (gain :hq-access 1)) :leave-play (effect (lose :hq-access 1))}

   "Inject"
   {:effect #(doseq [c (take 4 (get-in @%1 [:runner :deck]))]
               (if (= (:type c) "Program")
                 (do (trash %1 %2 c) (gain %1 %2 :credit 1)
                     (system-msg %1 %2 (str "trashes " (:title c) " and gains 1 [Credits]")))
                 (do (move %1 %2 c :hand) (system-msg %1 %2 (str "adds " (:title c) " to Grip")))))}

   "Jinteki: Personal Evolution"
   {:events {:agenda-scored {:msg "do 1 net damage" :effect (effect (damage :net 1))}
             :agenda-stolen {:msg "do 1 net damage" :effect (effect (damage :net 1))}}}

   "John Masanori"
   {:events {:successful-run {:msg "draw 1 card" :once :per-turn :once-key :john-masanori-draw
                              :effect (effect (draw))}
             :unsuccessful-run {:msg "take 1 tag" :once :per-turn :once-key :john-masanori-tag
                                :effect (effect (gain :tag 1))}}}

   "Investigative Journalism"
   {:req (req (> (:bad-publicity corp) 0))
    :abilities [{:cost [:click 4] :msg "give the Corp 1 bad publicity"
                 :effect (effect (gain :corp :bad-publicity 1) (trash card))}]}

   "Kati Jones"
   {:abilities
    [{:cost [:click 1] :msg "store 3 [Credits]" :once :per-turn
      :effect (effect (add-prop card :counter 3))}
     {:cost [:click 1] :msg (msg "gain " (:counter card) " [Credits]") :once :per-turn
      :label "Take all credits"
      :effect (effect (gain :credit (:counter card)) (set-prop card :counter 0))}]}

   "Lamprey"
   {:events {:successful-run {:req (req (= target :hq)) :msg "to force the Corp to lose 1 [Credits]"
                              :effect (effect (lose :corp :credit 1))}
             :purge {:effect (effect (trash card))}}}

   "Lawyer Up"
   {:effect (effect (draw 3) (lose :tag 2))}

   "Legwork"
   {:effect (effect (run :hq) (access-bonus 2))}

   "Levy AR Lab Access"
   {:effect (effect (shuffle-into-deck :hand :discard) (draw 5)
                    (move (first (:play-area runner)) :rfg))}

   "Liberated Account"
   {:data {:counter 16}
    :abilities [{:cost [:click 1] :counter-cost 4 :msg "gain 4 [Credits]"
                 :effect #(do (gain %1 :runner :credit 4)
                              (when (= (:counter %3) 0) (trash %1 :runner %3)))}]}

   "Lucky Find"
   {:effect (effect (gain :credit 9))}

   "Magnum Opus"
   {:abilities [{:cost [:click 1] :effect (effect (gain :credit 2)) :msg "gain 2 [Credits]"}]}

   "Mandatory Upgrades"
   {:effect (effect (gain :click 1 :click-per-turn 1))}

   "Marked Accounts"
   {:abilities [{:cost [:click 1] :message "store 3 [Credits]"
                 :effect (effect (add-prop card :counter 3))}]
    :events {:corp-turn-begins {:msg "gain 1 [Credits]" :counter-cost 1
                                :effect (effect (gain :credit 1))}}}

   "Market Research"
   {:req (req tagged) :effect (effect (set-prop card :counter 1 :agendapoints 3))}

   "Medical Research Fundraiser"
   {:effect (effect (gain :credit 8) (gain :runner :credit 3))}

   "Medium"
   {:events {:successful-run {:req (req (= target :rd))
                              :effect (effect (access-bonus (:counter card))
                                              (add-prop card :counter 1))}}}

   "Melange Mining Corp."
   {:abilities [{:cost [:click 3] :effect (effect (gain :credit 7)) :msg "gain 7 [Credits]"}]}

   "Mental Health Clinic"
   {:effect (effect (gain :runner :max-hand-size 1))
    :leave-play (effect (lose :runner :max-hand-size 1))
    :events {:corp-turn-begins {:msg "gain 1 [Credits]" :effect (effect (gain :credit 1))}}}

   "NAPD Contract"
   {:steal-cost [:credit 4]}

   "NBN: The World is Yours*"
   {:effect (effect (gain :max-hand-size 1))}

   "Near-Earth Hub: Broadcast Center"
   {:events {:server-created {:msg "draw 1 card" :once :per-turn :effect (effect (draw 1))}}}

   "Nerve Agent"
   {:events {:successful-run {:req (req (= target :hq))
                              :effect (effect (access-bonus (:counter card))
                                              (add-prop card :counter 1))}}}

   "Neural EMP"
   {:req (req (:made-run runner-reg)) :effect (effect (damage :net 1))}

   "Nisei MK II"
   {:data {:counter 1} :abilities [{:counter-cost 1 :msg "end the run" :effect (effect (end-run))}]}

   "Noise: Hacker Extraordinaire"
   {:events {:runner-install {:msg "trash the top card of R&D" :effect (effect (mill :corp))
                              :req (req (has? target :subtype "Virus"))}}}

   "Origami"
   {:effect (effect (gain :max-hand-size
                          (dec (* 2 (count (filter #(= (:title %) "Origami")
                                                   (get-in runner [:rig :program])))))))
    :leave-play (effect (lose :max-hand-size
                              (dec (* 2 (count (filter #(= (:title %) "Origami")
                                                       (get-in runner [:rig :program])))))))}

   "PAD Campaign"
   {:events {:corp-turn-begins {:msg "gain 1 [Credits]" :effect (effect (gain :credit 1))}}}

   "Order of Sol"
   {:effect #(add-watch % :order-of-sol
                        (fn [k ref old new]
                          (when (and (not (zero? (get-in old [:runner :credit])))
                                     (zero? (get-in new [:runner :credit])))
                            (resolve-ability ref %2 {:msg "gain 1 [Credits]" :once :per-turn
                                                    :effect (effect (gain :credit 1))} %3 nil))))
    :leave-play #(remove-watch % :order-of-sol)}

   "Paper Tripping"
   {:req (req (not (:spent-click runner-reg))) :effect (effect (lose :tag :all))}

   "Peak Efficiency"
   {:effect (effect (gain :credit
                          (reduce (fn [c server]
                                    (+ c (count (filter (fn [ice] (:rezzed ice)) (:ices server)))))
                                  0 (flatten (seq (:servers corp))))))}

   "Philotic Entanglement"
   {:msg (msg "do " (count (:scored runner)) " net damages")
    :effect (effect (damage :net (count (:scored runner))))}

   "Private Contracts"
   {:data {:counter 14}
    :abilities [{:cost [:click 1] :counter-cost 2 :msg "gain 2 [Credits]"
                 :effect #(do (gain %1 :corp :credit 2)
                              (when (= (:counter %3) 0)
                                (trash %1 :corp %3)))}]}

   "Private Security Force"
   {:abilities [{:req (req tagged) :cost [:click 1] :effect (effect (damage :meat 1))
                 :msg "do 1 meat damage"}]}

   "Public Sympathy"
   {:effect (effect (gain :max-hand-size 2)) :leave-play (effect (lose :max-hand-size 2))}

   "Power Nap"
   {:effect (effect (gain :credit (+ 2 (count (filter (fn [c] (has? c :subtype "Double"))
                                                      (:discard runner))))))}

   "Professional Contacts"
   {:abilities [{:cost [:click 1] :effect (effect (gain :credit 1) (draw))
                 :msg "gain 1 [Credits] and draw 1 card"}]}

   "Project Beale"
   {:effect (effect (set-prop card :counter (quot (- (:advance-counter card) 3) 2)
                                   :agendapoints (+ 2 (quot (- (:advance-counter card) 3) 2))))}

   "Quality Time"
   {:effect (effect (draw 5))}

   "R&D Interface"
   {:effect (effect (gain :rd-access 1)) :leave-play (effect (lose :rd-access 1))}

   "Rachel Beckman"
   {:effect #(do (gain % :runner :click 1 :click-per-turn 1)
                 (add-watch % :rachel-beckman
                            (fn [k ref old new]
                              (when (> (get-in new [:runner :tag]) 0)
                                (trash ref :runner %3)
                                (system-msg ref %2 "trash Rachel Beckman for being tagged")))))
    :leave-play #(do (remove-watch % :rachel-beckman)
                     (lose %1 %2 :click 1 :click-per-turn 1))}

   "Restructure"
   {:effect (effect (gain :credit 15))}

   "Reversed Accounts"
   {:advanceable :always
    :abilities [{:cost [:click 1]
                 :effect (effect (lose :runner :credit (* 4 (:advance-counter card))) (trash card))}]}
   "Ronin"
   {:advanceable :always
    :abilities [{:cost [:click 1] :req (req (>= (:advance-counter card) 4))
                 :effect (effect (damage :net 3) (trash card))}]}

   "Sentinel Defense Program"
   {:events {:damage {:req (req (= target :brain)) :msg "to do 1 net damage"
                      :effect (effect (damage :net 1)) }}}

   "Shell Corporation"
   {:abilities
    [{:cost [:click 1] :msg "store 3 [Credits]" :once :per-turn
      :effect (effect (add-prop card :counter 3))}
     {:cost [:click 1] :msg (msg "gain " (:counter card) " [Credits]") :once :per-turn
      :label "Take all credits"
      :effect (effect (gain :credit (:counter card)) (set-prop card :counter 0))}]}

   "Shock!"
   {:access {:msg "do 1 net damage" :effect (effect (damage :net 1))}}

   "Scorched Earth"
   {:req (req tagged) :effect (effect (damage :meat 4))}

   "Stim Dealer"
   {:events {:runner-turn-begins {:effect #(if (>= (:counter %3) 2)
                                             (do (set-prop %1 %2 %3 :counter 0)
                                                 (damage %1 %2 :brain 1)
                                                 (system-msg %1 %2 "takes 1 brain damage from Stim Dealer"))
                                             (do (add-prop %1 %2 %3 :counter 1)
                                                 (gain %1 %2 :click 1)
                                                 (system-msg %1 %2 "uses Stim Dealer to gain [Click]")))}}}

   "Subliminal Messaging"
   {:effect (effect (gain :credit 1)
                    (resolve-ability {:once :per-turn :once-key :subliminal-messaging
                                      :effect #(gain state :corp :click 1)} card nil))}

   "Successful Demonstration"
   {:req (req (:unsuccessful-run runner-reg)) :effect (effect (gain :credit 7))}

   "Sure Gamble"
   {:effect (effect (gain :credit 9))}

   "Sweeps Week"
   {:effect (effect (gain :credit (count (:hand runner))))}

   "TGTBT"
   {:access {:msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))}}

   "The Makers Eye"
   {:effect (effect (run :rd) (access-bonus 2))}

   "Theophilius Bagbiter"
   {:effect #(do (lose % :runner :credit :all)
                 (add-watch % :theophilius-bagbiter
                            (fn [k ref old new]
                              (let [credit (get-in new [:runner :credit])]
                                (when (not= (get-in old [:runner :credit]) credit)
                                  (swap! ref assoc-in [:runner :max-hand-size] credit))))))
    :leave-play #(remove-watch % :theophilius-bagbiter)}

   "Thomas Haas"
   {:advanceable :always
    :abilities [{:effect (effect (gain :credit (* 2 (:advance-counter card))) (trash card))}]}

   "Traffic Accident"
   {:req (req (>= (:tag runner) 2)) :effect (effect (damage :meat 2))}

   "Tri-maf Contact"
   {:abilities [{:cost [:click 1] :msg "gain 2 [Credits]" :once :per-turn
                 :effect (effect (gain :credit 2))}]
    :leave-play (effect (damage :meat 3))}

   "Turtlebacks"
   {:events {:server-created {:msg "gain 1 [Credits]" :effect (effect (gain :credit 1))}}}

   "Underworld Contact"
   {:events {:runner-turn-begins {:msg "gain 1 [Credits]" :req (req (>= (:link runner) 2))
                                  :effect (effect (gain :credit 1))}}}

   "Veterans Program"
   {:effect (effect (lose :bad-publicity 2))}

   "Vulcan Coverup"
   {:msg "do 2 meat damages" :effect (effect (damage :meat 2))
    :stolen {:msg "force the Corp to take 1 bad publicity"
             :effect (effect (gain :corp :bad-publicity 1))}}

   "Weyland Consortium: Building a Better World"
   {:events {:play-operation {:msg "gain 1 [Credits]" :effect (effect (gain :credit 1))
                              :req (req (has? target :subtype "Transaction"))}}}

   "Witness Tampering"
   {:effect (effect (lose :bad-publicity 2))}

   "Wyldside"
   {:events {:runner-turn-begins {:msg "draw 2 cards and lose [Click]"
                                  :effect (effect (lose :click 1) (draw 2))}}}

   ;; Icebreakers

   "Alpha"
   {:abilities [{:cost [:credit 1] :req (req (and run (zero? (:position run))))
                 :msg "break 1 subroutine on the outermost ICE protecting this server"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Alias"
   {:abilities [{:cost [:credit 1] :req (req (#{:hq :rd :archives} (first (:server run))))
                 :msg "break 1 sentry subroutine"}
                {:cost [:credit 2] :msg "add 3 strength" :effect (effect (pump card 3))}]}

   "Atman"
   {:abilities [{:cost [:credit 1] :msg "break 1 subroutine"}
                {:cost [:credit 1] :msg "place 1 power counter"
                 :effect (effect (add-prop card :counter 1))}]}

   "Aurora"
   {:abilities [{:cost [:credit 2] :msg "break 1 barrier subroutine"}
                {:cost [:credit 2] :msg "add 3 strength" :effect (effect (pump card 3))}]}

   "Battering Ram"
   {:abilities [{:cost [:credit 2] :msg "break up to 2 barrier subroutines"}
                {:cost [:credit 1] :msg "add 1 strength for the remainder of this run"
                 :effect (effect (pump card 1 true))}]}

   "BlacKat"
   {:abilities [{:cost [:credit 1] :msg "break 1 barrier subroutine"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Breach"
   {:abilities [{:cost [:credit 2] :req (req (#{:hq :rd :archives} (first (:server run))))
                 :msg "break 3 barrier subroutine"}
                {:cost [:credit 2] :msg "add 4 strength" :effect (effect (pump card 4))}]}

   "Cerberus \"Cuj.0\" H3"
   {:data {:counter 4}
    :abilities [{:counter-cost 1 :msg "break up to 2 sentry subroutines"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Cerberus \"Rex\" H2"
   {:data {:counter 4}
    :abilities [{:counter-cost 1 :msg "break up to 2 code gate subroutines"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Cerberus \"Lady\" H1"
   {:data {:counter 4}
    :abilities [{:counter-cost 1 :msg "break up to 2 barrier subroutines"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Corroder"
   {:abilities [{:cost [:credit 1] :msg "break 1 barrier subroutine"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Creeper"
   {:abilities [{:cost [:credit 2] :msg "break 1 sentry subroutine"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Crypsis"
   {:abilities [{:cost [:credit 1] :msg "break ICE subroutine"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}
                {:cost [:click 1] :msg "place 1 virus counter"
                 :effect (effect (add-prop card :counter 1))}
                {:counter-cost 1 :label "Remove 1 hosted virus counter" :msg "remove 1 virus counter"}]}

   "Cyber-Cypher"
   {:abilities [{:cost [:credit 1] :msg "break 1 code gate subroutine"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Dagger"
   {:abilities [{:cost [:credit 1] :msg "break 1 sentry subroutine"}
                {:cost [:credit 1] :msg "add 5 strength" :effect (effect (pump card 5))}]}

   "Darwin"
   {:abilities [{:cost [:credit 2] :msg "break ICE subroutine"}
                {:cost [:credit 1] :once :per-turn :msg "place 1 virus counter"
                 :effect (effect (add-prop card :counter 1))}]}

   "Deus X"
   {:abilities [{:msg "break any number of AP subroutine" :effect (effect (trash card))}
                {:msg "Prevent any number of net damage" :effect (effect (trash card))}]}

   "Faerie"
   {:abilities [{:msg "break any number of sentry subroutine" :effect (effect (trash card))}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Femme Fatale"
   {:abilities [{:cost [:credit 1] :msg "break 1 sentry subroutine"}
                {:cost [:credit 2] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Force of Nature"
   {:abilities [{:cost [:credit 2] :msg "break up to 2 code gate subroutines"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Garrote"
   {:abilities [{:cost [:credit 1] :msg "break 1 sentry subroutines"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Gordian Blade"
   {:abilities [{:cost [:credit 1] :msg "break 1 code gate subroutine"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Gingerbread"
   {:abilities [{:cost [:credit 1] :msg "break 1 tracer subroutine"}
                {:cost [:credit 2] :msg "add 3 strength" :effect (effect (pump card 3))}]}

   "Inti"
   {:abilities [{:cost [:credit 1] :msg "break 1 barrier subroutine"}
                {:cost [:credit 2] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Leviathan"
   {:abilities [{:cost [:credit 3] :msg "break up to 3 code gate subroutines"}
                {:cost [:credit 3] :msg "add 5 strength" :effect (effect (pump card 5))}]}

   "Morning Star"
   {:abilities [{:cost [:credit 1] :msg "break any number of barrier subroutines"}]}

   "Mimic"
   {:abilities [{:cost [:credit 1] :msg "break 1 sentry subroutine"}]}

   "Ninja"
   {:abilities [{:cost [:credit 1] :msg "break 1 sentry subroutine"}
                {:cost [:credit 3] :msg "add 5 strength" :effect (effect (pump card 5))}]}

   "Passport"
   {:abilities [{:cost [:credit 1] :req (req (#{:hq :rd :archives} (first (:server run))))
                 :msg "break 1 code gate subroutine"}
                {:cost [:credit 2] :msg "add 2 strength" :effect (effect (pump card 2))}]}

   "Omega"
   {:abilities [{:cost [:credit 1] :req (req (= (:position run) (dec (count (:ices run)))))
                 :msg "break 1 subroutine on the innermost ICE protecting this server"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Overmind"
   {:effect (effect (set-prop card :counter (:memory runner)))
    :abilities [{:counter-cost 1 :msg "break 1 subroutine"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Peacock"
   {:abilities [{:cost [:credit 2] :msg "break 1 code gate subroutine"}
                {:cost [:credit 2] :msg "add 3 strength" :effect (effect (pump card 3))}]}

   "Pipeline"
   {:abilities [{:cost [:credit 1] :msg "break 1 sentry subroutine"}
                {:cost [:credit 2] :msg "add 1 strength for the remainder of this run"
                 :effect (effect (pump card 1 true))}]}

   "Refractor"
   {:abilities [{:cost [:credit 1] :msg "break 1 code gate subroutine"}
                {:cost [:credit 1] :msg "add 3 strength" :effect (effect (pump card 3))}]}

   "Snowball"
   {:abilities [{:cost [:credit 1] :msg "break 1 barrier subroutine"}
                {:cost [:credit 1] :msg "add 1 strength for the remainder of the run"
                  :effect (effect (pump card 1 true))}]}

   "Sharpshooter"
   {:abilities [{:msg "break any number of destroyer subroutines" :effect (effect (trash card))}
                {:cost [:credit 1] :msg "add 2 strength" :effect (effect (pump card 2))}]}

   "Switchblade"
   {:abilities [{:cost [:credit 1] :msg "break any number sentry subroutines"}
                {:cost [:credit 1] :msg "add 7 strength" :effect (effect (pump card 7))}]}

   "Torch"
   {:abilities [{:cost [:credit 1] :msg "break 1 code gate subroutine"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Wyrm"
   {:abilities [{:cost [:credit 3] :msg "break 1 subroutine on ICE with 0 or less strength"}
                {:cost [:credit 1] :msg "reduce ICE strength by 1"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Yog.0"
   {:abilities [{:msg "break 1 code gate subroutine"}]}

   "ZU.13 Key Master"
   {:abilities [{:cost [:credit 1] :msg "break 1 code gate subroutine"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   ;; ICE
   "Ashigaru"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Bastion"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Caduceus"
   {:abilities [{:msg "gain 3 [Credits]" :effect (effect (gain :credit 3))}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Changeling"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Chimera"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Curtain Wall"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Data Mine"
   {:abilities [{:msg "do 1 net damage" :effect (effect (trash card) (damage :net 1))}]}

   "Datapike"
   {:abilities [{:msg "force the runner to pay 2 [Credits] if able"
                 :effect (effect (pay :runner :credit 2))}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Data Raven"
   {:abilities [{:msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))}
                {:msg "give the Runner 1 tag using 1 power counter"
                 :counter-cost 1 :effect (effect (gain :runner :tag 1))}
                {:msg "add 1 power counter" :effect (effect (add-prop card :counter 1))}]}

   "Eli 1.0"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Enigma"
   {:abilities [{:msg "force the runner to lose 1 [Click] if able"
                 :effect (effect (lose :runner :click 1))}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Fenris"
   {:effect (effect (gain :bad-publicity 1) (system-msg "takes 1 bad publicity"))
    :abilities [{:msg "do 1 brain damage" :effect (effect (damage :brain 1))}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Galahad"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Grim"
   {:effect (effect (gain :bad-publicity 1) (system-msg "takes 1 bad publicity"))}

   "Guard"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Hadrians Wall"
   {:advanceable :always
    :abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Himitsu-Bako"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Hive"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Heimdall 1.0"
   {:abilities [{:msg "do 1 brain damage" :effect (effect (damage :brain 1))}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Heimdall 2.0"
   {:abilities [{:msg "do 1 brain damage" :effect (effect (damage :brain 1))}
                {:msg "do 1 brain damage and end the run" :effect (effect (damage :brain 1) (end-run))}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Hourglass"
   {:abilities [{:msg "force the runner to lose 1 [Click] if able"
                 :effect (effect (lose :runner :click 1))}]}

   "Hunter"
   {:abilities [{:msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))}]}

   "Ice Wall"
   {:advanceable :always :abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "IQ"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Janus 1.0"
   {:abilities [{:msg "do 1 brain damage" :effect (effect (damage :brain 1))}]}

   "Komainu"
   {:abilities [{:msg "do 1 net damage" :effect (effect (damage :net 1))}]}

   "Lotus Field"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Mamba"
   {:abilities [{:msg "do 1 net damage" :effect (effect (damage :net 1))}
                {:msg "do 1 net damage using 1 power counter"
                 :counter-cost 1 :effect (effect (damage :net 1))}
                {:msg "add 1 power counter" :effect (effect (add-prop card :counter 1))}]}

   "Mother Goddess"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Neural Katana"
   {:abilities [{:msg "do 3 net damage" :effect (effect (damage :net 3))}]}

   "NEXT Bronze"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "NEXT Silver"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Paper Wall"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Pop-up Window"
   {:msg "gain 1 [Credits]" :effect (effect (gain :corp :credit 1))
    :abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Pup"
   {:abilities [{:msg "do 1 net damage" :effect (effect (damage :net 1))}]}

   "Quandary"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Rainbow"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Rototurret"
   {:abilities [{:msg "trash 1 program"}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Salvage"
   {:advanceable :while-rezzed
    :abilities [{:msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))}]}

   "Shadow"
   {:advanceable :always
    :abilities [{:msg "gain 2 [Credits]" :effect (effect (gain :credit 2))}
                {:msg "give the runner 1 tag" :effect (effect (gain :runner :tag 1))}]}

   "Shinobi"
   {:effect (effect (gain :bad-publicity 1) (system-msg "takes 1 bad publicity"))}

   "Swarm"
   {:advanceable :always
    :abilities [{:msg "trash 1 program unless the Runner pays 3 [Credits]"}]}

   "Swordsman"
   {:abilities [{:msg "do 1 net damage" :effect (effect (damage :net 1))}]}

   "Tyrant"
   {:advanceable :while-rezzed
    :abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "TMI"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Tollbooth"
   {:msg "force the runner to lose 3 [Credits]" :effect (effect (lose :runner :credit 3))
    :abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Universal Connectivity Fee"
   {:abilities [{:msg (msg "force the Runner to lose " (if (> (:tag runner) 0) "all credits" "1 [Credits]"))
                 :effect #(if (> (get-in @%1 [:runner :tag]) 0)
                            (do (lose %1 :runner :credit :all) (trash %1 %2 %3))
                            (lose %1 :runner :credit 1))}]}

   "Viktor 1.0"
   {:abilities [{:msg "do 1 brain damage" :effect (effect (damage :brain 1))}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Viktor 2.0"
   {:abilities [{:msg "do 1 brain damage using 1 power counter" :counter-cost 1
                 :effect (effect (damage :brain 1))}
                {:msg "add 1 power counter" :effect (effect (add-prop card :counter 1))}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Wall of Static"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Wall of Thorns"
   {:abilities [{:msg "do 2 net damage" :effect (effect (damage :net 2))}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Wendigo"
   {:advanceable :always
    :abilities [{:msg "prevent the runner from using a chosen program for the remaining of this run"}]}

   "Woodcutter"
   {:advanceable :while-rezzed
    :abilities [{:msg "do 1 net damage" :effect (effect (damage :net 1))}]}

   "Wotan"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Wraparound"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Yagura"
   {:abilities [{:msg "do 1 net damage" :effect (effect (damage :net 1))}]}

   "Zona Sul Shipping"
   {:events {:runner-turn-begins {:effect (effect (add-prop card :counter 1))}}
    :abilities [{:cost [:click 1] :msg (msg "gain " (:counter card) " [Credits]")
                 :label "Take all credits"
                 :effect (effect (gain :credit (:counter card)) (set-prop card :counter 0))}]
    :effect #(add-watch % (keyword (str "zona-sul-shipping" (:cid %3)))
                        (fn [k ref old new]
                          (when (> (get-in new [:runner :tag]) 0)
                            (trash ref :runner %3)
                            (system-msg ref %2 "trash Zona Sul Shipping for being tagged"))))
    :leave-play #(remove-watch % (keyword (str "zona-sul-shipping" (:cid %3))))}

   ;; partial implementation
   "AstroScript Pilot Program"
   {:data {:counter 1}}

   "Bad Times"
   {:req (req tagged)}

   "Bank Job"
   {:data {:counter 8}
    :abilities [{:counter-cost 1 :msg "gain 1 [Credits]" :req (req (:run @state))
                 :effect #(do (gain %1 :credit 1)
                              (when (zero? (:counter %3)) (trash %1 :runner %3)))}]}

   "Braintrust"
   {:effect (effect (set-prop card :counter (quot (- (:advance-counter card) 3) 2)))}

   "Breaking News"
   {:effect (effect (gain :runner :tag 2))
    :events {:corp-turn-end {:effect #(do (lose %1 :runner :tag 2)
                                          (unregister-event %1 %2 :corp-turn-ends %3))}}}

   "Chakana"
   {:events {:successful-run {:effect (effect (add-prop card :counter 1)) :req (req (= target :rd))}}}

   "D4v1d"
   {:data {:counter 3}}

   "Exile: Streethawk"
   {:effect (effect (gain :link 1))}

   "Datasucker"
   {:events {:successful-run {:effect (effect (add-prop card :counter 1))
                              :req (req (#{:hq :rd :archives} target))}}}
   "Deep Thought"
   {:events {:successful-run {:effect (effect (add-prop card :counter 1)) :req (req (= target :rd))}}}

   "Doppelgänger"
   {:effect (effect (gain :memory 1)) :leave-play (effect (lose :memory 1))}

   "Early Bird"
   {:req (req (not (:spent-click runner-reg)))}

   "Eden Shard"
   {:abilities [{:effect (effect (trash card) (draw :corp 2))
                 :msg "force the Corp to draw 2 cards"}]}

   "Efficiency Committee"
   {:data {:counter 3}
    :abilities [{:cost [:click 1] :counter-cost 1 :effect (effect (gain :click 2))
                 :msg "gain [Click][Click]"}]}

   "Emergency Shutdown"
   {:req (req (some #{:hq} (:successful-run runner-reg)))}

   "Fall Guy"
   {:abilities [{:effect (effect (trash card)) :msg "prevent another resource from being trashed"}
                {:effect (effect (trash card) (gain :credit 2)) :msg "gain 2 [Credits]"}]}

   "Freelancer"
   {:req (req tagged)}

   "Ghost Runner"
   {:data {:counter 3}
    :abilities [{:counter-cost 1 :msg "gain 1 [Credits]" :req (req (:run @state))
                 :effect #(do (gain %1 :credit 1)
                              (when (zero? (:counter %3)) (trash %1 :runner %3)))}]}

   "Hemorrhage"
   {:events {:successful-run {:effect (effect (add-prop card :counter 1))}}}

   "Iain Stirling: Retired Spook"
   {:effect (effect (gain :link 1))}

   "Imp"
   {:data {:counter 2}
    :abilities [{:counter-cost 1 :msg "trash at no cost" :once :per-turn :effect #()}]}

   "Jackson Howard"
   {:abilities [{:cost [:click 1] :effect (effect (draw 2)) :msg "draw 2 cards"}]}

   "Kate \"Mac\" McCaffrey: Digital Tinker"
   {:effect (effect (gain :link 1))}

   "Kraken"
   {:req (req (:stole-agenda runner-reg))}

   "Logos"
   {:effect (effect (gain :memory 1 :max-hand-size 1))
    :leave-play (effect (lose :memory 1 :max-hand-size 1))}

   "Midseason Replacements"
   {:req (req (:stole-agenda runner-reg))}

   "Monolith"
   {:effect (effect (gain :memory 3)) :leave-play (effect (lose :memory 3))}

   "Nasir Meidan: Cyber Explorer"
   {:effect (effect (gain :link 1))}

   "Networking"
   {:effect (effect (lose :tag 1))}

   "Power Shutdown"
   {:req (req (:made-run runner-reg))}

   "Project Atlas"
   {:effect (effect (set-prop card :counter (- (:advance-counter card) 3)))}

   "Project Vitruvius"
   {:effect (effect (set-prop card :counter (- (:advance-counter card) 3)))}

   "Psychographics"
   {:req (req tagged)}

   "Rabbit Hole"
   {:effect (effect (gain :link 1)) :leave-play (effect (lose :link 1))}

   "Reina Roja: Freedom Fighter"
   {:effect (effect (gain :link 1))}

   "Research Station"
   {:effect (effect (gain :max-hand-size 2))
    :leave-play (effect (lose :max-hand-size 2))}

   "SEA Source"
   {:req (req (:successful-run runner-reg))}

   "Spinal Modem"
   {:effect (effect (gain :memory 1)) :leave-play (effect (lose :memory 1))}

   "Tallie Perrault"
   {:abilities [{:cost [:click 1] :effect (effect (trash card) (draw (:bad-publicity corp)))
                 :msg (msg "draw " (:bad-publicity corp) " cards")}]
    :events {:play-operation {:msg "give the Corp 1 bad publicity and take 1 tag"
                              :effect (effect (gain :bad-publicity 1) (gain :runner :tag 1))
                              :req (req (or (has? target :subtype "Black Ops")
                                            (has? target :subtype "Gray Ops")))}}}

   "The Helpful AI"
   {:effect (effect (gain :link 1)) :leave-play (effect (lose :link 1))}

   "The Source"
   {:events {:agenda-scored (effect (trash card)) :agenda-stolen (effect (trash card))}}

   "The Toolbox"
   {:effect (effect (gain :link 2 :memory 2)) :leave-play (effect (lose :link 2 :memory 2))}

   "Three Steps Ahead"
   {:req (req (not (:spent-click runner-reg)))}})
