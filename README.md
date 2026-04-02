# SeaVerse - Karma Gerçeklik Destekli Okyanus Ekosistemi Öğrenme Oyunu

## 1. Proje Başlığı

**SeaVerse: Karma Gerçeklik Destekli Okyanus Ekosistemi Öğrenme Oyunu**

Müze ortamında çocuklara yönelik, fiziksel balık kartları ile dijital oyun deneyimini birleştiren tablet tabanlı bir eğitici eğlence (edutainment) uygulamasıdır.

## 2. Proje Özeti

SeaVerse, müzelerdeki deniz canlıları ve okyanus ekosistemi içeriklerini çocuklar için etkileşimli ve eğlenceli bir öğrenme deneyimine dönüştürmeyi hedefleyen bir Karma Gerçeklik uygulamasıdır.

Uygulama **"Tara → Dönüş → Hayatta Kal → Öğren → Yarış"** konsepti üzerine kurulmuştur. Her çocuğa bir balık kartı verilir. Çocuk bu kartı tablet kamerası ile okuttuğunda karttaki balık karakterine dönüşür. Ardından okyanus senaryolarında çeşitli kararlar vererek balığın yaşamını yönlendirir. Verilen kararlar sonucunda ekosistem, habitat, beslenme ve tehlikeler hakkında deneyimleyerek öğrenir.

## 3. Problem Tanımı

Müzelerde sunulan deniz canlıları ve okyanus ekosistemi içerikleri genellikle statik panolar ve görseller üzerinden aktarılmaktadır. Bu durum, özellikle çocuk kullanıcıların dikkatini yeterince çekmemekte ve öğrenme sürecinin pasif kalmasına neden olmaktadır. Çocuklar, deniz canlılarının yaşam koşullarını, davranışlarını ve ekosistem içerisindeki rollerini deneyimleyerek öğrenme fırsatı bulamamaktadır. Bu nedenle mevcut müze deneyimi, etkileşim ve kalıcı öğrenme açısından yetersiz kalmaktadır.

## 4. Amaçlar

- Müzelerdeki öğrenme deneyimini Karma Gerçeklik teknolojisi ile etkileşimli ve eğlenceli hale getirmek
- Çocukların deniz canlıları ve okyanus ekosistemi hakkında karar vererek ve sonuçlarını yaşayarak öğrenmesini sağlamak
- Fiziksel kartlar ile dijital oyun deneyimini birleştiren özgün bir edutainment sistemi geliştirmek
- Senaryo tabanlı karar mekanikleri ile çocukların problem çözme ve eleştirel düşünme becerilerini desteklemek
- Oyunlaştırma (gamification) yöntemleriyle öğrenmeyi kalıcı hale getirmek

## 5. Temel Kavramlar

**Karma Gerçeklik (Mixed Reality - MR):** Gerçek dünya ile dijital içeriklerin aynı ortamda bir arada bulunduğu ve birbirleriyle etkileşime girebildiği teknolojidir. Bu projede tablet kamerası aracılığıyla fiziksel kartların algılanması ve dijital balık karakterlerinin gerçek dünya ile entegre edilmesi şeklinde kullanılmaktadır. Projenin MR düzeyi, artırılmış gerçeklik tabanlı (AR ağırlıklı) bir Karma Gerçeklik uygulaması olarak değerlendirilebilir.

**Artırılmış Gerçeklik (Augmented Reality - AR):** Gerçek dünya görüntüsü üzerine dijital içeriklerin eklenmesiyle oluşturulan teknoloji. Bu projede kart tanıma ve balık karakteri görselleştirmede kullanılmaktadır.

**Edutainment (Eğitici Eğlence):** Eğitim ve eğlenceyi birleştiren yaklaşım. Çocuklar oyun oynarken farkında olmadan öğrenir.

**Oyunlaştırma (Gamification):** Oyun mekaniklerinin (puanlama, süre, karar verme) eğitim ortamlarında kullanılmasıdır.

**Dallanma Senaryosu (Branching Scenario):** Kullanıcının verdiği kararlara göre hikayenin farklı yönlere ilerlediği anlatı yapısıdır. Bu projede her karar, balığın habitat, beslenme ve tehlike bilgilerine göre farklı sonuçlar doğurur.

### MR Ekosistem Bileşenleri

| Bileşen | Açıklama | Örnek |
|---------|----------|-------|
| **Donanım** | Kullanıcının uygulamayı deneyimlediği fiziksel cihazlardır. Tabletin kamerası, sensörleri ve işlemcisi kartları algılayarak MR deneyimini başlatır. | Android tablet, tablet kamerası |
| **Yazılım** | Kartların tanınmasını, oyun senaryosunun çalışmasını ve balık karakterlerinin yönetilmesini sağlayan uygulama katmanıdır. | ARCore, Kotlin tabanlı Android uygulaması |
| **Etkileşim** | Kullanıcının sistemle kurduğu iletişim biçimidir. Kart okutma, ekranda seçenek belirleme ve karar verme mekanikleri ile sağlanır. | QR kod okutma, dokunmatik seçimler |

## 6. Kullanılan Teknolojiler

| Teknoloji | Kullanım Alanı |
|-----------|---------------|
| **Kotlin** | Android uygulama geliştirme dili |
| **Jetpack Compose** | Modern, deklaratif UI framework |
| **Compose Canvas** | 2D okyanus sahnesi, balık animasyonları ve görsel efektler |
| **ARCore** | Kart tanıma ve artırılmış gerçeklik katmanı |
| **ML Kit / ZXing** | QR kod okuma (MVP aşaması) |
| **Room / JSON** | Yerel veri saklama (balık profilleri, senaryolar) |
| **Android SDK** | Tablet uyumlu uygulama geliştirme platformu |

## 7. Proje Kapsamı

### Kapsam Dahilinde

- **Kart Tarama Sistemi:** QR kod ile balık kartı okuma ve kullanıcıya balık profili atama
- **Senaryo Motoru:** Dallanma senaryoları ile karar tabanlı oyun akışı
- **2D Okyanus Sahnesi:** Compose Canvas ile deniz ortamı, balık animasyonları, kabarcık efektleri ve paralaks katmanlar
- **Puanlama Sistemi:** Doğru/yanlış kararlar, hayatta kalma bonusu ve zamana dayalı skor
- **Öğrenme Enjeksiyonu:** Her karardan sonra ekosistem bilgisi aktarımı
- **Tek Oyunculu Mod:** Bireysel hikaye deneyimi
- **Tamamen Çevrimdışı Çalışma:** Müze ortamında internet bağımlılığı olmadan stabil performans

### Kapsam Dışında (Sonraki Aşamalar)

- Çok oyunculu mod (WiFi / WebSocket / Bluetooth)
- 3D balık avatarları ve gelişmiş AR görselleri
- Sesli anlatım (Text-to-Speech)
- Liderlik tablosu (müze ekranında)
- Kart koleksiyon sistemi
- Farklı müze temaları (dinozor, uzay vb.)

### Oyun Akışı

```
Kart Tarama → Balık Profili Yükleme → Karakter Tanıtımı → Senaryo Başlangıcı
→ Karar Verme → Sonuç ve Öğrenme → Yeni Senaryo → Skor Ekranı
```

### Senaryo Türleri

- Avcı karşılaşması
- Habitat değişikliği (tatlı su / tuzlu su)
- Besin kıtlığı
- Çevre kirliliği
- İnsan tehlikesi (balıkçı ağı vb.)

## 8. Beklenen Çıktılar

- Tablet üzerinde çalışan, çevrimdışı destekli Android uygulaması
- QR kod ile balık kartı tanıma ve profil atama sistemi
- En az 10 farklı balık türü için karakter profilleri
- Dallanma senaryoları ile etkileşimli öğrenme içerikleri
- 2D okyanus sahnesi ile görsel olarak çekici oyun deneyimi
- Zamana dayalı puanlama ve geri bildirim sistemi
- Çocuklara deniz ekosistemi, habitat, beslenme ve tehlikeler hakkında kalıcı bilgi aktarımı

## 9. Katkıda Bulunanlar

| Ad Soyad | Rol |
|----------|-----|
| Serhat Erdem | Proje Sahibi / Geliştirici |

## 10. Kaynaklar

- [ARCore Documentation - Google Developers](https://developers.google.com/ar)
- [Jetpack Compose - Android Developers](https://developer.android.com/jetpack/compose)
- [Canvas in Compose - Android Developers](https://developer.android.com/jetpack/compose/graphics/draw/overview)
- [ML Kit Barcode Scanning - Google Developers](https://developers.google.com/ml-kit/vision/barcode-scanning)
- [Gamification in Education - Deterding et al. (2011)](https://doi.org/10.1145/2181037.2181040)
- [Augmented Reality in Museums - Waltl et al.](https://doi.org/10.1007/978-3-319-49607-8_15)

## 11. Anahtar Kelimeler

`Karma Gerçeklik`, `Artırılmış Gerçeklik`, `Edutainment`, `Oyunlaştırma`, `Okyanus Ekosistemi`, `Deniz Canlıları`, `Etkileşimli Öğrenme`, `Android`, `Jetpack Compose`, `ARCore`, `Müze Teknolojisi`, `QR Kod`, `Senaryo Tabanlı Oyun`, `Tablet Uygulaması`

---

### SWOT Analizi

| Güçlü Yönler | Zayıf Yönler |
|--------------|--------------|
| Eğitim ile oyunlaştırmayı birleştirerek çocukların ilgisini artırır | Tablet veya mobil cihaz gerektirdiği için donanım bağımlılığı vardır |
| Karma gerçeklik teknolojisi ile etkileşimli öğrenme deneyimi sunar | Yazılım geliştirme ve içerik üretimi zaman ve teknik bilgi gerektirir |
| Fiziksel kartlar ile dijital deneyimi birleştirerek kullanıcı katılımını artırır | Çok küçük yaş grupları için kullanım rehberliği gerekebilir |
| Müzelerde klasik sergi anlayışını deneyimsel hale getirir | İlk kurulum ve içerik tasarımı zaman alabilir |

| Fırsatlar | Tehditler |
|-----------|----------|
| Müzelerde ve bilim merkezlerinde eğitim aracı olarak kullanılabilir | Benzer eğitim teknolojisi uygulamaları rekabet oluşturabilir |
| Teknoloji destekli eğitim uygulamalarına ilgi artmaktadır | Cihazlarda teknik arızalar kullanımı etkileyebilir |
| Farklı ekosistemler eklenerek genişletilebilir | Donanım maliyetleri bazı kurumlar için sınırlayıcı olabilir |
| Okullar ve eğitim kurumlarında kullanılabilir | Teknolojiye alışık olmayan kullanıcılar zorluk yaşayabilir |
