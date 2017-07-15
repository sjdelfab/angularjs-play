FROM openjdk:8
COPY angularjs-play-1.0-SNAPSHOT.zip /opt/
RUN cd /opt/ && unzip angularjs-play-1.0-SNAPSHOT.zip && ln -s angularjs-play-1.0-SNAPSHOT angularjs-play
RUN rm /opt/angularjs-play-1.0-SNAPSHOT.zip
RUN rm -rf /opt/angularjs-play/share
RUN rm /opt/angularjs-play/README.md
RUN useradd -r webapp
RUN chown -R webapp /opt/angularjs-play
RUN chmod -R u+rxw /opt/angularjs-play
COPY wait-for-it.sh /opt/
RUN chmod a+x /opt/wait-for-it.sh
CMD ["/opt/wait-for-it.sh","db:5432","--","/opt/angularjs-play/bin/angularjs-play"]
