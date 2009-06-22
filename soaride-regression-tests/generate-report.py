import os

dirs = [d for d in os.listdir('.') if os.path.isdir(d) and not d[0] == '.']
readmes = [os.path.join(d, 'readme.txt') for d in dirs if os.path.exists(os.path.join(d, 'readme.txt'))]
readmes.sort()

result = open('soaride-test-report.txt', 'w')

for readme in readmes:
   f = open(readme, 'r')
   lines = f.readlines()
   result.write('-------------------------------------------------------\n')
   result.write(readme + '\n')
   result.write('-------------------------------------------------------\n')
   
   inVerify = False
   verifyString = ''
   for line in lines:
      if inVerify:
         if line[0] == ' ' or line[0] == '\t' or line[0:2] == '**':
            verifyString += line
         else:
            result.write(verifyString)
            result.write('   Result: []\n')
            result.write('   Notes:\n\n')
            inVerify = False
            if line.find('Verify') >= 0:
               inVerify = True
               verifyString = line
            else:
               result.write(line)
      else:
         if line.find('Verify') >= 0:
            inVerify = True
            verifyString = line
         else:
            result.write(line)
         
   result.write('\n\n')
   f.close()

result.close()

