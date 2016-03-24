(ns gps-tracker.styles)

(def GREY "#C0C0C0")

(def PURPLE "#660066")

(def GOLD "#e6bc06")

(def styles
  (js.React.StyleSheet.create
   (clj->js
    {:button {:padding 5
              :margin 5
              :borderRadius 5
              :backgroundColor GREY}
     :goldBorder {:borderWidth 5
                  :borderColor GOLD}
     :narrowButton {:marginHorizontal 40}
     :text {:textAlign "center"}
     :bigText {:fontSize 20}
     :middle {:top 125}
     :center {:margin 40}
     :timeBox {:marginTop 30
               :marginHorizontal 5
               :borderRadius 5
               :backgroundColor GREY
               :padding 5}
     :toolbar {:height 56
               :backgroundColor GREY}
     :scroll-view {:height 300}
     :marginVertical {:marginVertical 10}
     :purple {:backgroundColor PURPLE}
     :page {:margin 40}
     :fullPage {:height 512}})))
