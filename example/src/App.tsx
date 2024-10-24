import { StyleSheet, View } from 'react-native';

import { RecyclerListView } from '../../src';

const extendedDummyData = () => {
  const dummyData = [];
  for (let i = 0; i <= 1000; i++) {
    dummyData[i] = { title: `Test View ${i}` };
  }
  return dummyData;
};
export default function App() {
  return (
    <View style={styles.container}>
      <RecyclerListView
        data={extendedDummyData()}
        columns={3}
        style={styles.recyclerView}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1, // Ensures the container takes all available space
    justifyContent: 'center',
    alignItems: 'center',
  },
  recyclerView: {
    flex: 1, // Ensures the RecyclerView fills the container
    width: '100%', // Optional: Ensures full width
  },
});
