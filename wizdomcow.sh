# source: https://gist.github.com/higgs241/2404ae435e59423f96f44354d57e8dec

# _________________________________________ 
#/ Never let your schooling interfere with \
#\ your education.                         /
# ----------------------------------------- 
#        \   ^__^
#         \  (oo)\_______
#            (__)\       )\/\
#                ||----w |
#                ||     ||
# 

# run with bash wizdomcow.sh

# install homebrew
/usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"

# install fortune
brew install fortune

# install cowsay
brew install cowsay

# make the dopest command ever
printf "\nfortune | cowsay\n" >> ~/.bash_profile
