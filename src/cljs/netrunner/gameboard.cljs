(ns netrunner.gameboard
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as sab :include-macros true]
            [cljs.core.async :refer [chan put! <!] :as async]
            [clojure.string :refer [capitalize]]
            [netrunner.main :refer [app-state]]
            [netrunner.auth :refer [avatar] :as auth]
            [netrunner.cardbrowser :refer [image-url add-symbols] :as cb]))

(defonce game-state (atom {}))
(defonce lock (atom false))

(defn init-game [game side]
  (swap! game-state merge game)
  (swap! game-state assoc :side side))

(def zoom-channel (chan))
(def socket (.connect js/io (str js/iourl "/lobby")))
(def socket-channel (chan))
(.on socket "netrunner" #(put! socket-channel (js->clj % :keywordize-keys true)))

(go (while true
      (let [msg (<! socket-channel)]
        (case (:type msg)
          "state" (do (swap! game-state merge (:state msg)) (reset! lock false))
          nil))))

(defn send [msg]
  (.emit socket "netrunner" (clj->js msg)))

(defn send-command
  ([command] (send-command command nil))
  ([command args]
     (when-not @lock
       (try (js/ga "send" "event" "game" command) (catch js/Error e))
       (reset! lock true)
       (send {:action "do" :gameid (:gameid @game-state) :side (:side @game-state)
              :command command :args args}))))

(defn send-msg [event owner]
  (.preventDefault event)
  (let [input (om/get-node owner "msg-input")
        text (.-value input)]
    (when-not (empty? text)
      (send-command "say" {:text text})
      (aset input "value" "")
      (.focus input))))

(defn action-list [{:keys [type zone rezzed advanceable advance-counter advancementcost] :as card}]
  (-> []
      (#(if (or (and (= type "Agenda") (= (first zone) "servers"))
                (= advanceable "always")
                (and rezzed (= advanceable "while-rezzed"))
                (and (not rezzed) (= advanceable "while-unrezzed")))
          (cons "advance" %) %))
      (#(if (and (= type "Agenda") (>= advance-counter advancementcost))
          (cons "score" %) %))
      (#(if (and (#{"Asset" "ICE" "Upgrade"} type) (not rezzed))
          (cons "rez" %) %))))

(defn handle-abilities [{:keys [abilities] :as card} owner]
  (let [actions (action-list card)
        c (+ (count actions) (count abilities))]
    (cond (> c 1) (-> (om/get-node owner "abilities") js/$ .toggle)
          (= c 1) (if (= (count abilities) 1)
                        (send-command "ability" {:card card :ability 0})
                        (send-command (first actions) {:card card})))))

(defn handle-card-click [{:keys [type zone counter advance-counter advancementcost advanceable] :as card} owner]
  (if (= (:side @game-state) :runner)
    (case (first zone)
      "hand" (send-command "play" {:card card})
      "rig" (handle-abilities card owner)
      nil)
    (case (first zone)
      "hand" (case type
               ("Upgrade" "ICE") (-> (om/get-node owner "servers") js/$ .toggle)
               ("Agenda" "Asset") (if (empty? (get-in @game-state [:corp :servers :remote]))
                                    (send-command "play" {:card card :server "New remote"})
                                    (-> (om/get-node owner "servers") js/$ .toggle))
               (send-command "play" {:card card}))
      ("servers" "scored") (handle-abilities card owner)
      nil)))

(defn in-play? [card]
  (let [dest (when (= (:side card) "Runner")
               (get-in @game-state [:runner :rig (keyword (.toLowerCase (:type card)))]))]
    (some #(= (:title %) (:title card)) dest)))

(defn playable? [{:keys [title side zone cost type uniqueness abilities memoryunits] :as card}]
  (let [my-side (:side @game-state)
        me (my-side @game-state)]
    (and (= (keyword (.toLowerCase side)) my-side)
         (and (= zone ["hand"])
              (or (not uniqueness) (not (in-play? card)))
              (or (#{"Agenda" "Asset" "Upgrade" "ICE"} type) (>= (:credit me) cost))
              (or (not memoryunits) (<= memoryunits (:memory me)))
              (> (:click me) 0)))))

(defn log-pane [messages owner]
  (reify
    om/IDidUpdate
    (did-update [this prev-props prev-state]
      (let [div (om/get-node owner "msg-list")]
        (aset div "scrollTop" (.-scrollHeight div))))

    om/IRenderState
    (render-state [this state]
      (sab/html
       [:div.log
        [:div.messages.panel.blue-shade {:ref "msg-list"}
         (for [msg messages]
           (if (= (:user msg) "__system__")
             [:div.system {:dangerouslySetInnerHTML #js {:__html (add-symbols (:text msg))}}]
             [:div.message
              (om/build avatar (:user msg) {:opts {:size 38}})
              [:div.content
               [:div.username (get-in msg [:user :username])]
               [:div (:text msg)]]]))]
        [:form {:on-submit #(send-msg % owner)}
         [:input {:ref "msg-input" :placeholder "Say something"}]]]))))

(defn remote-list []
  (map #(str "Server " %) (-> (get-in @game-state [:corp :servers :remote]) count range reverse)))

(defn handle-dragstart [e cursor]
  (-> e .-target js/$ (.addClass "dragged"))
  (-> e .-dataTransfer (.setData "card" (JSON/stringify (clj->js @cursor)))))

(defn handle-drop [e server]
  (-> e .-target js/$ (.removeClass "dragover"))
  (let [card (-> e .-dataTransfer (.getData "card") JSON/parse (js->clj :keywordize-keys true))]
    (send-command "move" {:card card :server server})))

(defn card-view [{:keys [zone code type abilities counter advance-counter advancementcost subtype
                         advanceable rezzed strength current-strength] :as cursor}
                 owner {:keys [flipped] :as opts}]
  (om/component
   (when code
     (sab/html
      [:div.blue-shade.card {:draggable true
                             :on-drag-start #(handle-dragstart % cursor)
                             :on-drag-end #(-> % .-target js/$ (.removeClass "dragged"))
                             :on-mouse-enter #(when (or (not flipped) (= (:side @game-state) :corp))
                                                (put! zoom-channel cursor))
                             :on-mouse-leave #(put! zoom-channel false)
                             :on-click #(handle-card-click @cursor owner)}
       (when-let [url (image-url cursor)]
         (if flipped
           [:img.card.bg {:src "/img/corp.png"}]
           [:img.card.bg {:src url :onError #(-> % .-target js/$ .hide)}]))
       [:div.counters
        (when (> counter 0) [:div.darkbg.counter counter])
        (when (> advance-counter 0) [:div.darkbg.advance.counter advance-counter])]
       (when current-strength [:div.darkbg.strength current-strength])
       (when (and (= zone ["hand"]) (#{"Agenda" "Asset" "ICE" "Upgrade"} type))
         (let [centrals ["HQ" "R&D" "Archives"]
               remotes (conj (remote-list) "New remote")
               servers (case type
                         ("Upgrade" "ICE") (concat remotes centrals)
                         ("Agenda" "Asset") remotes)]
           [:div.blue-shade.panel.servers-menu {:ref "servers"}
            (map (fn [label]
                   [:div {:on-click #(do (send-command "play" {:card @cursor :server label})
                                         (-> (om/get-node owner "servers") js/$ .fadeOut))}
                    label])
                 servers)]))
       (let [actions (action-list cursor)]
         (when (> (+ (count actions) (count abilities)) 1)
           [:div.blue-shade.panel.abilities {:ref "abilities"}
            (map (fn [action]
                   [:div {:on-click #(do (send-command action {:card @cursor}))} (capitalize action)])
                 actions)
            (map-indexed
             (fn [i label]
               [:div {:on-click #(do (send-command "ability" {:card @cursor :ability i})
                                     (-> (om/get-node owner "abilities") js/$ .fadeOut))
                      :dangerouslySetInnerHTML #js {:__html (add-symbols label)}}])
             abilities)]))
       (when (= (first zone) "servers")
         (cond
          (and (= type "Agenda") (>= advance-counter advancementcost))
          [:div.blue-shade.panel.menu.abilities {:ref "agenda"}
           [:div {:on-click #(send-command "advance" {:card @cursor})} "Advance"]
           [:div {:on-click #(send-command "score" {:card @cursor})} "Score"]]
          (or (= advanceable "always") (and rezzed (= advanceable "rezzed-only")))
          [:div.blue-shade.panel.menu.abilities {:ref "advance"}
           [:div {:on-click #(send-command "advance" {:card @cursor})} "Advance"]
           [:div {:on-click #(send-command "rez" {:card @cursor})} "Rez"]]))]))))

(defn label [cursor owner opts]
  (om/component
   (sab/html
    (let [fn (or (:fn opts) count)]
      [:div.header {:class (when (> (count cursor) 0) "darkbg")}
       (str (:name opts) " (" (fn cursor) ")")]))))

(defn hand-view [{:keys [identity hand max-hand-size user] :as cursor}]
  (om/component
   (sab/html
    (let [side (:side identity)
          size (count hand)]
      [:div.panel.blue-shade.hand {:class (when (> size 6) "squeeze")}
       (om/build label hand {:opts {:name (if (= side "Corp") "HQ" "Grip")}})
       (map-indexed (fn [i card]
                      (sab/html
                       [:div.card-wrapper {:class (if (playable? card) "playable" "")
                                           :style {:left (* (/ 320 (dec size)) i)}}
                        (if (= user (:user @app-state))
                          (om/build card-view card)
                          [:img.card {:src (str "/img/" (.toLowerCase side) ".png")}])]))
                    hand)]))))

(defmulti deck-view #(get-in % [:identity :side]))

(defmethod deck-view "Runner" [{:keys [deck] :as cursor}]
  (om/component
   (sab/html
    [:div.panel.blue-shade.deck {}
     (om/build label deck {:opts {:name "Stack"}})
     (when (> (count deck) 0)
       [:img.card.bg {:src "/img/runner.png"}])])))

(defmethod deck-view "Corp" [{:keys [deck] :as cursor}]
  (om/component
   (sab/html
    [:div.panel.blue-shade.deck {}
     (om/build label deck {:opts {:name "R&D"}})
     (when (> (count deck) 0)
       [:img.card.bg {:src "/img/corp.png"}])])))

(defn drop-area [side server]
  (when (= (:side @game-state) side)
    {:on-drop #(handle-drop % server)
     :on-drag-enter #(-> % .-target js/$ (.addClass "dragover"))
     :on-drag-leave #(-> % .-target js/$ (.removeClass "dragover"))
     :on-drag-over #(.preventDefault %)}))

(defmulti discard-view #(get-in % [:identity :side]))

(defmethod discard-view "Runner" [{:keys [discard] :as cursor} owner]
  (om/component
   (sab/html
    [:div.panel.blue-shade.discard (drop-area :runner "Heap")
     (om/build label discard {:opts {:name "Heap"}})
     (when-not (empty? discard)
       (om/build card-view (last discard)))])))

(defmethod discard-view "Corp" [{:keys [discard] :as cursor}]
  (om/component
   (sab/html
    [:div.panel.blue-shade.discard (drop-area :corp "Archives")
     (om/build label discard {:opts {:name "Archives"}})
     (when-not (empty? discard)
       (om/build card-view (last discard)))])))

(defn rfg-view [{:keys [rfg] :as cursor}]
  (om/component
   (sab/html
    (let [size (count rfg)]
      (when (> size 0)
        [:div.panel.blue-shade.rfg {:class (when (> size 3) "squeeze")}
         (om/build label rfg {:opts {:name "Removed"}})
         (map-indexed (fn [i card]
                        (sab/html
                         [:div.card-wrapper {:style {:left (* (/ 128 (dec size)) i)}}
                          [:div (om/build card-view card)]]))
                      rfg)])))))

(defn scored-view [{:keys [scored] :as cursor}]
  (om/component
   (sab/html
    (let [size (count scored)]
      [:div.panel.blue-shade.scored {:class (when (> size 3) "squeeze")}
       (om/build label scored {:opts {:name "Scored Area"}})
       (map-indexed (fn [i card]
                      (sab/html
                       [:div.card-wrapper {:style {:left (* (/ 128 (dec size)) i)}}
                        [:div (om/build card-view card)]]))
                    scored)]))))

(defn controls [key]
  (sab/html
   [:div.controls
    [:button.small {:on-click #(send-command "change" {:key key :delta 1}) :type "button"} "+"]
    [:button.small {:on-click #(send-command "change" {:key key :delta -1}) :type "button"} "-"]]))

(defmulti stats-view #(get-in % [:identity :side]))

(defmethod stats-view "Runner" [{:keys [user click credit memory link tag brain-damage agenda-point
                                        max-hand-size]} owner]
  (om/component
   (sab/html
    (let [me? (= (:side @game-state) :runner)]
      [:div.stats.panel.blue-shade {}
       [:h4.ellipsis (om/build avatar user {:opts {:size 22}}) (:username user)]
       [:div (str click " Click" (if (> click 1) "s" "")) (when me? (controls :click))]
       [:div (str credit " Credit" (if (> credit 1) "s" "")) (when me? (controls :credit))]
       [:div (str memory " Memory Unit" (if (> memory 1) "s" "")) (when me? (controls :memory))]
       [:div (str link " Link" (if (> link 1) "s" "")) (when me? (controls :link))]
       [:div (str agenda-point " Agenda Point" (when (> agenda-point 1) "s"))
        (when me? (controls :agenda-point))]
       [:div (str tag " Tag" (if (> tag 1) "s" "")) (when me? (controls :tag))]
       [:div (str brain-damage " Brain Damage" (if (> brain-damage 1) "s" ""))
        (when me? (controls :brain-damage))]
       [:div (str max-hand-size " Max hand size") (when me? (controls :max-hand-size))]]))))

(defmethod stats-view "Corp" [{:keys [user click credit agenda-point bad-publicity max-hand-size]} owner]
  (om/component
   (sab/html
    (let [me? (= (:side @game-state) :corp)]
      [:div.stats.panel.blue-shade {}
       [:h4.ellipsis (om/build avatar user {:opts {:size 22}}) (:username user)]
       [:div (str click " Click" (if (> click 1) "s" "")) (when me? (controls :click))]
       [:div (str credit " Credit" (if (> credit 1) "s" "")) (when me? (controls :credit))]
       [:div (str agenda-point " Agenda Point" (when (> agenda-point 1) "s"))
        (when me? (controls :agenda-point))]
       [:div (str bad-publicity " Bad Publicit" (if (> bad-publicity 1) "ies" "y"))
        (when me? (controls :bad-publicity))]
       [:div (str max-hand-size " Max hand size") (when me? (controls :max-hand-size))]]))))

(defn server-view [{:keys [server run] :as cursor} owner opts]
  (om/component
   (sab/html
    (let [content (:content server)]
      [:div.server 
       (let [ices (:ices server)]
         [:div.ices
          (when run
            [:div.run-arrow {:style {:top (str (+ 8 (* 64 (- (count ices) (:position run)))) "px")}}])
          (for [ice ices]
            (om/build card-view ice {:opts {:flipped (not (:rezzed ice))}}))])
       (when content
         [:div.content {:class (when (= (count content) 1) "center")}
          (for [card (reverse content)]
            (om/build card-view card {:opts {:flipped (not (:rezzed card))}}))
          (when content
            (om/build label content {:opts opts}))])]))))

(defmulti board-view #(get-in % [:player :identity :side]))

(defmethod board-view "Corp" [{:keys [player run]}]
  (om/component
   (sab/html
    (let [servers (:servers player)
          s (:server run)
          server-type (first s)]
      [:div.corp-board {:class (when (= (:side @game-state) :runner) "opponent")}
       (om/build server-view {:server (:archives servers) :run (when (= server-type "archives") run)})
       (om/build server-view {:server (:rd servers) :run (when (= server-type "rd") run)})
       (om/build server-view {:server (:hq servers) :run (when (= server-type "hq") run)})
       (map-indexed
        (fn [i server]
          (om/build server-view {:server server
                                 :run (when (and (= server-type "remote")
                                                 (= (js/parseInt (second s)) i)) run)}
                    {:opts {:name (str "Server " i)}}))
        (:remote servers))]))))

(defmethod board-view "Runner" [{:keys [player run]}]
  (om/component
   (sab/html
    [:div.runner-board
     (for [zone [:program :resource :hardware]]
       [:div (for [c (zone (:rig player))]
               [:div.card-wrapper {:class (when (playable? c) "playable")}
                (om/build card-view c)])])])))

(defn zones [cursor]
  (om/component
   (sab/html
    [:div.dashboard
     (om/build hand-view cursor)
     (om/build discard-view cursor)
     (om/build deck-view cursor)
     [:div.panel.blue-shade.identity
      (om/build card-view (:identity cursor))]])))

(defn cond-button [text cond f]
  (sab/html
   (if cond
     [:button {:on-click f} text]
     [:button.disabled text])))

(defn handle-end-turn [cursor owner]
  (let [me ((:side @game-state) @game-state)
        max-size (:max-hand-size me)]
    (if (> (count (:hand me)) max-size)
      (om/set-state! owner :warning (str "Discard to " max-size " cards"))
      (do (om/set-state! owner :warning nil)
          (send-command "end-turn")))))

(defn gameboard [{:keys [side gameid active-player run end-turn] :as cursor} owner]
  (reify
    om/IWillMount
    (will-mount [this]
      (go (while true
            (let [card (<! zoom-channel)]
              (om/set-state! owner :zoom card)))))

    om/IRenderState
    (render-state [this state]
      (sab/html
       (when (> gameid 0)
         (let [me (side cursor)
               opponent ((if (= side :corp) :runner :corp) cursor)]
           [:div.gameboard
            [:div.mainpane
             (om/build zones opponent)

             [:div.centralpane
              [:div.leftpane
               [:div
                (om/build stats-view opponent)
                (om/build scored-view opponent)
                (om/build rfg-view opponent)]
               [:div
                (om/build rfg-view me)
                (om/build scored-view me)
                (om/build stats-view me)]]

              [:div.button-pane
               (when-not (:keep me)
                 [:div.panel.blue-shade
                  [:h4 "Keep hand?"]
                  [:button {:on-click #(send-command "keep")} "Keep"]
                  [:button {:on-click #(send-command "mulligan")} "Mulligan"]])

               (when (:keep me)
                 (if-let [prompt (first (:prompt me))]
                   [:div.panel.blue-shade
                    [:h4 {:dangerouslySetInnerHTML #js {:__html (add-symbols (:msg prompt))}}]
                    (for [c (:choices prompt)]
                      [:button {:on-click #(send-command "choice" {:choice c})} c])]
                   (if run
                     (let [s (:server run)
                           kw (keyword (first s))
                           server (if-let [n (second s)]
                                    (get-in cursor [:corp :servers kw n])
                                    (get-in cursor [:corp :servers kw]))]
                       (if (= side :runner)
                         [:div.panel.blue-shade
                          (when-not (:no-action run) [:h4 "Waiting for Corp's actions" ])
                          (if (= (:position run) (count (:ices server)))
                            (cond-button "Access" (:no-action run) #(send-command "access"))
                            (cond-button "Continue" (:no-action run) #(send-command "continue")))
                          [:button {:on-click #(send-command "jack-out")} "Jack Out"]]
                         [:div.panel.blue-shade
                          (cond-button "No more action" (not (:no-action run))
                                       #(send-command "no-action"))]))
                     [:div.panel.blue-shade
                      (when-let [warning (:warning state)] [:h4 warning])
                      (if (= (keyword active-player) side)
                        (when (and (zero? (:click me)) (not end-turn))
                          [:button {:on-click #(handle-end-turn cursor owner)} "End Turn"])
                        (when end-turn
                          [:button {:on-click #(send-command "start-turn")} "Start Turn"]))
                      (when (= side :runner)
                        [:div
                         (cond-button "Remove Tag"
                                      (and (>= (:click me) 1) (>= (:credit me) 2) (>= (:tag me) 1))
                                      #(send-command "remove-tag"))
                         [:div.run-button
                          (cond-button "Run" (>= (:click me) 1)
                                       #(-> (om/get-node owner "servers") js/$ .toggle))
                          (let [servers (concat (remote-list) ["HQ" "R&D" "Archives"])]
                            [:div.blue-shade.panel.servers-menu {:ref "servers"}
                             (map (fn [label]
                                    [:div {:on-click #(do (send-command "run" {:server label})
                                                          (-> (om/get-node owner "servers") js/$ .fadeOut))}
                                     label])
                                  servers)])]])
                      (when (= side :corp)
                        (cond-button "Purge" (>= (:click me) 3) #(send-command "purge")))
                      (cond-button "Draw" (>= (:click me) 1) #(send-command "draw"))
                      (cond-button "Gain Credit" (>= (:click me) 1) #(send-command "credit"))])))]

              [:div.board
               (om/build board-view {:player opponent :run run})
               (om/build board-view {:player me :run run})]]
             (om/build zones me)]
            [:div.rightpane {}
             [:div.card-zoom
              (when-let [card (om/get-state owner :zoom)]
                [:img.card.bg {:src (image-url card)}])]
             (om/build log-pane (:log cursor))]]))))))

(om/root gameboard game-state {:target (. js/document (getElementById "gameboard"))})
