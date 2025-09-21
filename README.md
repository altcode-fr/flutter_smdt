# smdt

Flutter plugin for SMDT library

Les fichiers de signatures ont été fournis par le fournisseur chinois des puces Rockchip.
Ils ne sont pas publiés sur le repo.

Le plugin utilise deux versions de la librairie SMDT, une version pour les appareils Android <= Android 10, et une version pour les appareils avec une version Android > Android 10.
Les librairies smdt_old.jar et smdt_new.jar sont chargées dynamiquement au runtime afin d'appeler les méthodes correspondantes à la version de Android présent sur le matériel en cours d'exécution.