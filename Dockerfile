# Utiliser une image OpenJDK
FROM openjdk:11-jdk

# Installer les outils nécessaires (curl, unzip)
RUN apt-get update && apt-get install -y curl unzip wget && rm -rf /var/lib/apt/lists/*

# Télécharger et installer Android SDK command-line tools
RUN wget https://dl.google.com/android/repository/commandlinetools-linux-7583922_latest.zip -O /tmp/tools.zip \
    && unzip /tmp/tools.zip -d /sdk/cmdline-tools \
    && rm /tmp/tools.zip

# Accepter les licences et installer les outils Android
RUN yes | /sdk/cmdline-tools/cmdline-tools/bin/sdkmanager --sdk_root=/sdk --licenses \
    && /sdk/cmdline-tools/cmdline-tools/bin/sdkmanager "platform-tools" "platforms;android-30"

# Définir les variables d'environnement pour Android SDK
ENV ANDROID_HOME /sdk
ENV PATH $ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/cmdline-tools/bin:$PATH

# Copier les fichiers de votre projet dans l'image
COPY . .

# Définir le répertoire de travail
WORKDIR /workspace

# Commande par défaut
CMD ["bash"]
