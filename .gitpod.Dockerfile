FROM gitpod/workspace-full

USER gitpod

# Install Clojure
RUN curl -O https://download.clojure.org/install/linux-install-1.10.1.536.sh \
    && chmod +x linux-install-1.10.1.536.sh  \
    && sudo ./linux-install-1.10.1.536.sh 

# Install rlwrap
RUN sudo apt-get install -y rlwrap

# Install asciidoc for project doc previews
RUN sudo apt-get install -y  asciidoctor

# Install Lein
# RUN curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > lein \
#     && sudo mv lein /usr/local/bin/lein \
#     && sudo chmod a+x /usr/local/bin/lein


# Install Joker
# RUN curl https://github.com/candid82/joker/releases/download/v0.15.4/joker-0.15.4-linux-amd64.zip > joker.zip \
#     && unzip joker.zip \
#     && sudo mv joker /usr/local/bin/joker\
#     && sudo chmod a+x /usr/local/bin/joker

# Give access back to gitpod image builder
USER root