### Creation of the masc_vocab.txt file.

 - obtain MASC 3.0.0
 - get all the text files together

$ find . -name "*.txt" -exec cat {} \; > all.txt

 - get the counts

$ tr 'A-Z' 'a-z' < all.txt | tr -cs 'a-z' '\n' | sort | uniq -c | sort -nr > counts.txt

 - output all the words that occurred 10 or more times and have three or more characters

$ cat counts.txt | awk '{ if ($1>=10 && length($2)>2) print $2}' > masc_vocab.txt

